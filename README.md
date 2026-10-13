# paridade

Codificação por apagamento do zero em Java 21: **tolerar as mesmas perdas não é ter a mesma
durabilidade.**

O juiz é a **varredura de todos os padrões de perda**. Para cada esquema ele apaga todo
subconjunto de fragmentos que existe, pede o dado de volta e compara byte por byte. Com catorze
fragmentos são 16.384 tentativas; somando os nove esquemas das tabelas, **18.572**.

Ele é o juiz certo e não só o conveniente porque não sabe nada sobre o esquema: não usa a matriz,
não usa k nem m, não confia em propriedade alegada nenhuma. E ele separa as duas coisas que todo
mundo mistura:

- **recusar** é devolver null porque perdeu demais, e isso está certo;
- **errar** é devolver bytes que não são o dado, e isso é o único resultado inaceitável, porque
  num sistema de verdade significa um arquivo corrompido em silêncio.

Ele exige **correção, não eficiência**. Espaço, tráfego e durabilidade são as tabelas, e elas só
querem dizer alguma coisa depois que o esquema está certo.

A unidade é o **fragmento perdido**, nunca o segundo. Um fragmento perdido é `null` e não um vetor
de zeros, e essa diferença é o assunto: aqui se trata de **apagamento**, em que se sabe quem
sumiu, e não de corrupção, em que o byte mente sem avisar.

## Metade do espaço, a mesma tolerância

```
$ mvn exec:java -Dexec.args="correcao"

  esquema            frag    padroes    recuperou      recusou      ERROU  tolera sempre
  copias 2              2          4            3            1          0              1
  copias 3              3          8            7            1          0              2
  copias 5              5         32           31            1          0              4
  paridade 4+1          5         32            6           26          0              1
  paridade 8+1          9        512           10          502          0              1
  RS 4+2                6         64           22           42          0              2
  RS 6+3                9        512          130          382          0              3
  RS 8+2               10       1024           56          968          0              2
  RS 10+4              14      16384         1471        14913          0              4

  18572 padroes de perda tentados ao todo, 0 com dado errado.
```

A coluna `tolera sempre` é medida e não alegada: é o maior j tal que **todos** os padrões de j
perdas recuperaram. Reed-Solomon com m paridades dá exatamente m, que é o teorema, conferido
subconjunto por subconjunto.

E aí vem a conta que vende Reed-Solomon:

```
$ mvn exec:java -Dexec.args="espaco"

  esquema            frag       espaco  tolera sempre   espaco por perda   padroes que vive
  copias 3              3       3.0000              2             1.5000             0.8750
  RS 4+2                6       1.5000              2             0.7500             0.3438
  RS 10+4              14       1.4000              4             0.3500             0.0898
```

`RS 4+2` tolera as mesmas duas perdas que três cópias e gasta **metade** do espaço. `RS 10+4`
tolera o dobro e gasta menos da metade. Parece que a conversa acabou.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `espaco`, `durabilidade`, `reconstrucao` e `largura`.

## O que as medidas me corrigiram

**Tolerar as mesmas perdas não é ter a mesma durabilidade. `RS 4+2` tolera duas perdas igual a
três cópias, gasta metade do espaço, e é 20 vezes mais provável de perder o dado.**

```
$ mvn exec:java -Dexec.args="durabilidade"

   chance frag esquema              espaco   tolera chance de perder      noves
         0.010 copias 3             3.0000        2        1.000e-06       6.00
         0.010 RS 4+2               1.5000        2        1.955e-05       4.71
         0.010 RS 8+2               1.2500        2        1.138e-04       3.94
         0.010 RS 6+3               1.5000        3        1.210e-06       5.92
         0.010 RS 10+4              1.4000        4        1.857e-07       6.73
         0.010 copias 5             5.0000        4        1.000e-10      10.00
```

A razão é de uma linha e eu não a tinha visto: **três cópias são três coisas que podem falhar, e
`RS 4+2` são seis.** Tolerar duas perdas de seis é mais difícil que tolerar duas perdas de três,
porque há mais jeitos de três delas sumirem.

Veja `RS 8+2`: mesmas duas perdas toleradas, 1,25 de espaço, e **113 vezes** mais provável de
perder o dado que três cópias. Quanto mais fino o código, mais fragmentos, mais chances de a
terceira falha chegar.

Essa tabela não vem de fórmula. Para cada padrão de perda que o juiz reprovou, soma-se a chance
daquele padrão exato acontecer. A conta de sempre, "tolera m, então é a cauda da binomial a
partir de m mais um", vale para um código separável de distância máxima e não vale para a
replicação, que recupera de montes de padrões de peso alto. Somar sobre o que o juiz reprovou
vale para os dois.

### A volta por cima, quando a comparação é honesta

Comparar `RS 4+2` com três cópias era a comparação errada: elas toleram o mesmo e **não compram a
mesma coisa**. Comparando por espaço, que é o que se paga:

| | espaço | chance de perder | |
|---|---|---|---|
| três cópias | 3,0 | 1,000e-06 | |
| `RS 2+4` | 3,0 | 5,950e-10 | **1.680 vezes mais durável** |
| `RS 10+4` | 1,4 | 1,857e-07 | 5,4 vezes mais durável, por menos da metade |

No **mesmo espaço exato**, quatro paridades sobre dois fragmentos de dado são três ordens de
grandeza melhores que três cópias. E `RS 10+4`, que gasta 1,4 em vez de 3,0, continua ganhando.

Então a frase certa não é "Reed-Solomon é melhor" nem "é pior". É que **o número de paridades é
que compra durabilidade, e a largura do código é que compra espaço**, e trocar um pelo outro sem
dizer isso é o jeito de chegar a `RS 4+2` achando que está empatando com três cópias.

### E contar padrões de perda aponta para o lado contrário

A coluna `padroes que vive` da tabela de espaço diz que três cópias sobrevivem a **87,5%** de
todos os padrões de perda possíveis e que `RS 10+4` sobrevive a **9,0%**. Pela contagem, a
replicação ganha de lavada.

Pela durabilidade, `RS 10+4` ganha por 5,4 vezes.

As duas medidas não se contradizem: elas pesam os padrões de forma diferente. Contar trata perder
treze de catorze fragmentos como tão importante quanto perder um. Com 1% de chance por fragmento,
quase toda a probabilidade mora nos padrões de **poucas** perdas, e é lá que o Reed-Solomon
recupera tudo.

Contar padrões é a medida intuitiva, é a que dá para calcular de cabeça, e aponta para o lado
errado.

### Uma correção menor, sobre o corpo finito

Eu escrevi o gerador do corpo de 256 elementos como **3**, com um comentário dizendo que 2 não
funcionaria. É exatamente o contrário, e o engano é fácil: 3 é o gerador do corpo do padrão de
cifragem, que usa o polinômio 0x11B. Aqui o polinômio é o 0x11D, do padrão de disco.

Com 0x11D, as potências de 3 percorrem **51** valores e voltam ao um. A tabela de logaritmo
ficaria com 204 posições em branco, todo produto que caísse nelas daria zero, e a matriz de
Vandermonde não inverteria.

Foi assim que apareceu: o construtor do Reed-Solomon levantou "as primeiras linhas da Vandermonde
nao inverteram" antes de imprimir uma linha de tabela. Hoje há um teste que conta os 255 contra os
51, e outro que confere o produto por tabela contra o produto ingênuo nos **65.536** pares
possíveis, porque tabela é justamente onde um erro de um índice passa despercebido.

## O que a tabela de espaço não mostra

```
$ mvn exec:java -Dexec.args="reconstrucao"

  esquema           frag em bytes    bytes lidos     amplificacao       espaco
  copias 3                     16             16             1.00       3.0000
  copias 5                     16             16             1.00       5.0000
  paridade 4+1                 16             64             4.00       1.2500
  RS 4+2                       16             64             4.00       1.5000
  RS 10+4                      16            160            10.00       1.4000
```

Repor uma cópia perdida é **copiar uma cópia**: lê-se exatamente o que se vai escrever. Repor um
fragmento de `RS 10+4` é ler **dez** fragmentos e decodificar.

Isso não aparece em folheto nenhum e é o que decide na prática, porque reconstrução não é evento
raro: num conjunto grande de discos sempre há algum sendo reposto, e a amplificação vira tráfego
de rede todo dia. É a razão prática de a replicação continuar viva onde a conta de espaço diz que
ela não deveria.

## Alargar o código mexe em três eixos, e dois pioram

```
$ mvn exec:java -Dexec.args="largura"

     dados  paridades     frag     espaco   tolera chance de perder      noves   amplificacao
         2          2        4     2.0000        2        3.970e-06       5.40           2.00
         4          2        6     1.5000        2        1.955e-05       4.71           4.00
         6          2        8     1.3333        2        5.393e-05       4.27           6.00
         8          2       10     1.2500        2        1.138e-04       3.94           8.00
        10          2       12     1.2000        2        2.056e-04       3.69          10.00
         2          4        6     3.0000        4        5.950e-10       9.23           2.00
         4          4        8     2.0000        4        5.461e-09       8.26           4.00
         6          4       10     1.6667        4        2.417e-08       7.62           6.00
         8          4       12     1.5000        4        7.470e-08       7.13           8.00
        10          4       14     1.4000        4        1.857e-07       6.73          10.00
```

Com duas paridades fixas, ir de dois para dez fragmentos de dado economiza espaço (2,0 para 1,2) e
cobra em tudo mais: a durabilidade cai de 5,40 para 3,69 noves e a reconstrução passa a ler cinco
vezes mais. **A coluna `tolera` não mexe**, e é justamente ela que a maioria das conversas usa
para comparar.

E as duas metades da tabela dizem quem manda: o código mais largo com **quatro** paridades
(6,73 noves, espaço 1,4) ganha do mais estreito com **duas** (5,40 noves, espaço 2,0) em
durabilidade **e** em espaço. Paridade compra durabilidade; largura compra espaço.

## O que não tem aqui

**Falhas correlacionadas.** A conta de durabilidade supõe que os fragmentos somem de forma
independente, e eles não somem: um disco, um bastidor, uma fonte, uma atualização de software
derrubam vários de uma vez. Essa é a hipótese mais generosa possível com códigos de muitos
fragmentos, então os números daqui favorecem Reed-Solomon mais do que a realidade favoreceria.

**Corrupção silenciosa.** Aqui se sabe quem sumiu. Um byte que muda sem avisar é outro problema e
custa o dobro de redundância: um código que tolera m apagamentos tolera só m dividido por dois
erros, porque metade da informação é descobrir **onde** está o erro.

**O tempo de reconstrução.** Enquanto um fragmento está sendo reposto, o código está com menos
redundância do que anuncia. Um código que leva um dia para reconstruir e tolera quatro perdas
passa esse dia tolerando três.

**Velocidade.** Nada aqui é cronometrado. Na prática o ou-exclusivo da paridade simples é ordens
de grandeza mais rápido que a multiplicação no corpo finito, e é por isso que ela existe separada
em vez de ser `RS k+1`.

## Licença

MIT.
