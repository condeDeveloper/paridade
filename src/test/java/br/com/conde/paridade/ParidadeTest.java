package br.com.conde.paridade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** As pecas, uma a uma, antes de qualquer medida. */
class ParidadeTest {

    // ───────────────────── o corpo ─────────────────────

    @Nested
    @DisplayName("o corpo de 256 elementos")
    class OCorpo {

        /**
         * O JUIZ DE DENTRO: o produto rapido contra o produto devagar, nos 65.536 pares.
         *
         * <p>Nao e uma amostra. O produto rapido e tabela, e tabela e exatamente onde um
         * erro de um indice passa despercebido num teste por amostragem.
         */
        @Test
        @DisplayName("o produto por tabela bate com o produto ingenuo em todos os 65.536 pares")
        void osDoisProdutosConcordam() {
            for (int a = 0; a < 256; a++)
                for (int b = 0; b < 256; b++)
                    assertThat(Campo.produto(a, b))
                            .as(a + " vezes " + b)
                            .isEqualTo(Campo.produtoDevagar(a, b));
        }

        /**
         * O GERADOR PRECISA SER PRIMITIVO, e tres nao e com este polinomio.
         *
         * <p>Este teste existe porque eu escrevi tres primeiro. Tres e o gerador do corpo do
         * padrao de cifragem, que usa 0x11B. Com 0x11D ele percorre 51 valores e volta.
         */
        @Test
        @DisplayName("dois percorre os 255 valores e tres percorre 51, com este polinomio")
        void soDoisEhPrimitivo() {
            assertThat(quantosValoresPercorre(2))
                    .as("dois e primitivo em 0x11D, e e por isso que ele e o gerador")
                    .isEqualTo(255);

            assertThat(quantosValoresPercorre(3))
                    .as("e tres volta ao um depois de 51, deixando 204 logaritmos em branco")
                    .isEqualTo(51);

            assertThat(Campo.GERADOR).isEqualTo(2);
        }

        private int quantosValoresPercorre(int gerador) {
            java.util.Set<Integer> vistos = new java.util.HashSet<>();

            int atual = 1;

            for (int i = 0; i < 255; i++) {
                vistos.add(atual);
                atual = Campo.produtoDevagar(atual, gerador);
            }

            return vistos.size();
        }

        @Test
        @DisplayName("todo elemento nao nulo tem inverso, e o produto pelo inverso da um")
        void todoMundoTemInverso() {
            for (int a = 1; a < 256; a++)
                assertThat(Campo.produto(a, Campo.inverso(a))).as("" + a).isEqualTo(1);
        }

        @Test
        @DisplayName("zero nao tem inverso, aqui como em qualquer lugar")
        void zeroNaoTemInverso() {
            assertThatThrownBy(() -> Campo.inverso(0))
                    .isInstanceOf(ArithmeticException.class)
                    .hasMessageContaining("zero nao tem inverso");
        }

        @Test
        @DisplayName("somar e o ou-exclusivo, e por isso somar duas vezes desfaz")
        void somarDuasVezesDesfaz() {
            for (int a = 0; a < 256; a++)
                for (int b = 0; b < 256; b++)
                    assertThat(Campo.soma(Campo.soma(a, b), b)).as(a + " e " + b).isEqualTo(a);
        }

        @Test
        @DisplayName("o produto e distributivo sobre a soma, que e o que faz a matriz valer")
        void aDistributiva() {
            for (int a = 0; a < 256; a += 7)
                for (int b = 0; b < 256; b += 11)
                    for (int c = 0; c < 256; c += 13)
                        assertThat(Campo.produto(a, Campo.soma(b, c)))
                                .as(a + ", " + b + ", " + c)
                                .isEqualTo(Campo.soma(Campo.produto(a, b), Campo.produto(a, c)));
        }
    }

    // ───────────────────── a matriz ─────────────────────

    @Nested
    @DisplayName("a matriz")
    class AMatriz {

        /**
         * A PROPRIEDADE QUE FAZ REED-SOLOMON EXISTIR, conferida subconjunto por subconjunto.
         *
         * <p>Com catorze linhas e dez colunas ha 1.001 jeitos de escolher dez linhas. Todos
         * os 1.001 tem que inverter, porque cada um deles e um padrao de sobrevivencia.
         */
        @Test
        @DisplayName("quaisquer 10 das 14 linhas da Vandermonde formam matriz inversivel")
        void quaisquerKLinhasInvertem() {
            Matriz vandermonde = Matriz.vandermonde(14, 10);

            int testados = 0;

            for (int escolha = 0; escolha < (1 << 14); escolha++) {
                if (Integer.bitCount(escolha) != 10) continue;

                int[] quais = new int[10];
                int quantos = 0;

                for (int i = 0; i < 14; i++)
                    if ((escolha & (1 << i)) != 0) quais[quantos++] = i;

                assertThat(vandermonde.linhas(quais).inversa())
                        .as("linhas " + java.util.Arrays.toString(quais))
                        .isNotNull();

                testados++;
            }

            assertThat(testados).as("e sao os 1.001 subconjuntos, nao uma amostra").isEqualTo(1_001);
        }

        @Test
        @DisplayName("a inversa vezes a original da a identidade")
        void aInversaDesfaz() {
            int[] quais = {3, 1, 7, 0, 9};

            Matriz pedaco = Matriz.vandermonde(12, 5).linhas(quais);

            Matriz produto = pedaco.vezes(pedaco.inversa());

            for (int i = 0; i < 5; i++)
                for (int j = 0; j < 5; j++)
                    assertThat(produto.em(i, j)).as(i + "," + j).isEqualTo(i == j ? 1 : 0);
        }

        @Test
        @DisplayName("uma matriz com linha repetida e singular, e a inversa devolve null")
        void aSingularDevolveNull() {
            Matriz repetida = new Matriz(new int[][] {{1, 2}, {1, 2}});

            assertThat(repetida.inversa())
                    .as("null aqui quer dizer que os fragmentos que sobraram nao bastam")
                    .isNull();
        }

        @Test
        @DisplayName("o corpo tem 256 elementos, e um codigo sobre bytes nao passa de 256 fragmentos")
        void naoCabeMaisQue256() {
            assertThatThrownBy(() -> Matriz.vandermonde(257, 4))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("256 nos distintos");
        }

        @Test
        @DisplayName("multiplicar matrizes de tamanhos que nao casam reclama")
        void tamanhosQueNaoCasam() {
            assertThatThrownBy(() -> Matriz.identidade(3).vezes(Matriz.identidade(4)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao da para multiplicar");
        }
    }

    // ───────────────────── o sorteio ─────────────────────

    @Nested
    @DisplayName("o sorteio da casa")
    class OSorteio {

        /** O mesmo gerador sem o deslocamento, para o teste poder mostrar a diferenca. */
        private byte[] bytesSemDeslocamento(long semente, int quantos) {
            long estado = semente;

            byte[] saida = new byte[quantos];

            for (int i = 0; i < quantos; i++) {
                estado = estado * 6364136223846793005L + 1442695040888963407L;
                saida[i] = (byte) estado;
            }

            return saida;
        }

        /**
         * O DEFEITO DOS BITS DE BAIXO ARRUINARIA O JUIZ, e este teste conta o estrago.
         *
         * <p>Sem o deslocamento os oito bits de baixo repetem com periodo 256. Um dado de
         * 320 bytes partido em fragmentos de 16 fica com fragmentos IDENTICOS, e um
         * decodificador que trocasse dois de lugar passaria na comparacao byte por byte.
         */
        @Test
        @DisplayName("sem o deslocamento, fragmentos diferentes do mesmo dado ficam identicos")
        void semODeslocamentoOsFragmentosSeRepetem() {
            assertThat(paresIdenticos(bytesSemDeslocamento(Medidas.SEMENTE, 320), 16))
                    .as("o periodo e 256, entao os bytes de 256 em diante repetem os primeiros")
                    .isEqualTo(4);

            assertThat(paresIdenticos(new Sorteio(Medidas.SEMENTE).bytes(320), 16))
                    .as("com os bits de cima nao ha um par igual")
                    .isZero();
        }

        private int paresIdenticos(byte[] dados, int porFragmento) {
            int fragmentos = dados.length / porFragmento;

            int pares = 0;

            for (int i = 0; i < fragmentos; i++)
                for (int j = i + 1; j < fragmentos; j++) {
                    boolean iguais = true;

                    for (int t = 0; t < porFragmento; t++)
                        if (dados[i * porFragmento + t] != dados[j * porFragmento + t]) {
                            iguais = false;
                            break;
                        }

                    if (iguais) pares++;
                }

            return pares;
        }

        @Test
        @DisplayName("o sorteio da os mesmos numeros em qualquer maquina")
        void oSorteioEhFixo() {
            Sorteio sorteio = new Sorteio(1);

            assertThat(List.of(sorteio.proximo(), sorteio.proximo(), sorteio.proximo()))
                    .containsExactly(1817669548, -2107078989, -1510284903);
        }
    }

    // ───────────────────── os esquemas ─────────────────────

    @Nested
    @DisplayName("os esquemas")
    class OsEsquemas {

        private final byte[] dados = new Sorteio(Medidas.SEMENTE).bytes(10 * Medidas.BYTES);

        @Test
        @DisplayName("sem perda nenhuma, todo esquema devolve o dado identico")
        void semPerdaVoltaIgual() {
            for (Esquema esquema : Medidas.esquemas()) {
                byte[] meus = new Sorteio(Medidas.SEMENTE).bytes(esquema.bytesDeDado());

                assertThat(esquema.decodificar(esquema.codificar(meus)))
                        .as(esquema.nome())
                        .isEqualTo(meus);
            }
        }

        /**
         * A FORMA SISTEMATICA: os k primeiros fragmentos SAO o dado.
         *
         * <p>E por isso que ler um arquivo sem perda nenhuma nao custa decodificacao
         * nenhuma, que e o que torna Reed-Solomon usavel na pratica.
         */
        @Test
        @DisplayName("em Reed-Solomon os k primeiros fragmentos sao o dado em si")
        void aFormaSistematica() {
            ReedSolomon esquema = new ReedSolomon(10, 4, Medidas.BYTES);

            byte[][] fragmentos = esquema.codificar(dados);

            for (int i = 0; i < 10; i++)
                for (int t = 0; t < Medidas.BYTES; t++)
                    assertThat(fragmentos[i][t])
                            .as("fragmento " + i + " byte " + t)
                            .isEqualTo(dados[i * Medidas.BYTES + t]);

            Matriz codificadora = esquema.codificadora();

            for (int i = 0; i < 10; i++)
                for (int j = 0; j < 10; j++)
                    assertThat(codificadora.em(i, j)).isEqualTo(i == j ? 1 : 0);
        }

        @Test
        @DisplayName("a paridade simples e o ou-exclusivo de todos os outros fragmentos")
        void aParidadeEhOuExclusivo() {
            ParidadeSimples esquema = new ParidadeSimples(4, Medidas.BYTES);

            byte[] meus = new Sorteio(7).bytes(esquema.bytesDeDado());

            byte[][] fragmentos = esquema.codificar(meus);

            for (int t = 0; t < Medidas.BYTES; t++) {
                int soma = 0;

                for (int i = 0; i < 4; i++) soma ^= fragmentos[i][t] & 0xFF;

                assertThat((byte) soma).as("byte " + t).isEqualTo(fragmentos[4][t]);
            }
        }

        @Test
        @DisplayName("com duas perdas a paridade simples recusa, e nao chuta")
        void aParidadeRecusaDuasPerdas() {
            ParidadeSimples esquema = new ParidadeSimples(4, Medidas.BYTES);

            byte[] meus = new Sorteio(7).bytes(esquema.bytesDeDado());

            byte[][] fragmentos = esquema.codificar(meus);

            fragmentos[1] = null;
            fragmentos[3] = null;

            assertThat(esquema.decodificar(fragmentos))
                    .as("duas incognitas e uma equacao nao tem solucao, e ele sabe disso")
                    .isNull();
        }

        @Test
        @DisplayName("um dado do tamanho errado e erro de quem chamou, e nao de quem guarda")
        void tamanhoErrado() {
            assertThatThrownBy(() -> new ReedSolomon(4, 2, 16).codificar(new byte[10]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("recebeu 10");
        }

        @Test
        @DisplayName("um codigo sobre bytes nao passa de 256 fragmentos, e o construtor recusa")
        void codigoLargoDemais() {
            assertThatThrownBy(() -> new ReedSolomon(250, 10, 16))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("256 fragmentos");
        }
    }

    // ───────────────────── o juiz ─────────────────────

    @Nested
    @DisplayName("o juiz")
    class OJuiz {

        private final byte[] dados = new Sorteio(Medidas.SEMENTE).bytes(4 * Medidas.BYTES);

        /** Um esquema que, perdendo demais, inventa zeros em vez de dizer que perdeu. */
        private Esquema queChuta() {
            ParidadeSimples honesto = new ParidadeSimples(4, Medidas.BYTES);

            return new Esquema() {
                @Override
                public String nome() {
                    return "chuta zeros";
                }

                @Override
                public int fragmentos() {
                    return honesto.fragmentos();
                }

                @Override
                public int bytesDeDado() {
                    return honesto.bytesDeDado();
                }

                @Override
                public int bytesGuardados() {
                    return honesto.bytesGuardados();
                }

                @Override
                public byte[][] codificar(byte[] entrada) {
                    return honesto.codificar(entrada);
                }

                @Override
                public byte[] decodificar(byte[][] fragmentos) {
                    byte[] voltou = honesto.decodificar(fragmentos);

                    /* Em vez de recusar, devolve zeros: o pior resultado possivel. */
                    return voltou != null ? voltou : new byte[bytesDeDado()];
                }

                @Override
                public int bytesLidosParaReconstruirUm() {
                    return honesto.bytesLidosParaReconstruirUm();
                }
            };
        }

        @Test
        @DisplayName("o juiz pega um esquema que inventa dado em vez de recusar")
        void pegaOQueChuta() {
            Juiz.Veredito veredito = Juiz.julgar(queChuta(), dados);

            assertThat(veredito.certo()).isFalse();

            assertThat(veredito.errados())
                    .as("os 26 padroes que o honesto recusaria viram dado errado")
                    .isEqualTo(26);

            assertThat(veredito.recusados()).isZero();

            assertThat(Juiz.primeiroPadraoErrado(queChuta(), dados)).isNotEqualTo(-1);
        }

        @Test
        @DisplayName("os esquemas honestos nao erram em padrao nenhum")
        void osHonestosNaoErram() {
            for (Esquema esquema : Medidas.esquemas()) {
                byte[] meus = new Sorteio(Medidas.SEMENTE).bytes(esquema.bytesDeDado());

                assertThat(Juiz.primeiroPadraoErrado(esquema, meus))
                        .as(esquema.nome())
                        .isEqualTo(-1);
            }
        }

        @Test
        @DisplayName("o juiz tenta exatamente dois elevado ao numero de fragmentos")
        void tentaTodosOsPadroes() {
            for (Esquema esquema : Medidas.esquemas()) {
                Juiz.Veredito veredito = Medidas.veredito(esquema);

                assertThat(veredito.padroes())
                        .as(esquema.nome())
                        .isEqualTo(1L << esquema.fragmentos());

                assertThat(veredito.recuperados() + veredito.recusados() + veredito.errados())
                        .as(esquema.nome() + ": todo padrao cai em exatamente um balde")
                        .isEqualTo(veredito.padroes());
            }
        }

        @Test
        @DisplayName("perder tudo nunca recupera nada, em esquema nenhum")
        void perderTudoNuncaVolta() {
            for (Esquema esquema : Medidas.esquemas()) {
                Juiz.Veredito veredito = Medidas.veredito(esquema);

                assertThat(veredito.recuperadosPorPeso()[esquema.fragmentos()])
                        .as(esquema.nome())
                        .isZero();

                assertThat(veredito.recuperadosPorPeso()[0])
                        .as(esquema.nome() + ": e sem perda nenhuma sempre volta")
                        .isEqualTo(1);
            }
        }

        @Test
        @DisplayName("o juiz recusa uma varredura que nao terminaria nesta vida")
        void varreduraGrandeDemais() {
            assertThatThrownBy(() -> Juiz.julgar(new ReedSolomon(30, 4, 2), new byte[60]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao e uma varredura");
        }
    }
}
