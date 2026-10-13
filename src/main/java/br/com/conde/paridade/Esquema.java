package br.com.conde.paridade;

/**
 * UM ESQUEMA DE GUARDA: como transformar um dado em fragmentos e como voltar.
 *
 * <p>Todos aqui respondem a mesma pergunta, que e a unica que importa: perdendo ESTES
 * fragmentos, ainda da para ter o dado de volta, byte por byte?
 *
 * <p>A resposta tem exatamente tres valores possiveis, e o terceiro e o que faz este
 * repositorio existir:
 *
 * <ul>
 *   <li>Recuperou, e os bytes conferem.
 *   <li>Recusou: devolveu null porque sabe que nao da.
 *   <li>ERROU: devolveu bytes que nao sao os do dado. Esse e o unico resultado inaceitavel,
 *       porque um sistema que devolve dado errado em silencio e pior que um que diz que
 *       perdeu. O juiz procura exatamente isso, em todo padrao de perda que existe.
 * </ul>
 *
 * <p>Um fragmento perdido e null, e nao um vetor de zeros. A diferenca e o assunto: este
 * repositorio trata de APAGAMENTO, em que se sabe QUEM sumiu, e nao de corrupcao, em que o
 * byte mente sem avisar. Saber quem sumiu e metade da informacao, e e por isso que um
 * codigo de apagamento aguenta o dobro de perdas de um codigo de correcao com a mesma
 * redundancia.
 */
public interface Esquema {

    String nome();

    /** Quantos fragmentos sao guardados. */
    int fragmentos();

    /** Quantos bytes de dado este esquema recebe de uma vez. */
    int bytesDeDado();

    /** Quantos bytes sao guardados no total, somando todos os fragmentos. */
    int bytesGuardados();

    /** Quanto se guarda por byte de dado. Um ponto zero seria nao ter redundancia nenhuma. */
    default double espacoPorByte() {
        return (double) bytesGuardados() / bytesDeDado();
    }

    byte[][] codificar(byte[] dados);

    /** O dado de volta, ou null quando o que sobrou nao basta. Null em fragmento e perda. */
    byte[] decodificar(byte[][] fragmentos);

    /**
     * Quantos bytes e preciso LER para reconstruir um fragmento perdido.
     *
     * <p>Esta conta nao aparece em nenhum folheto e e a que decide na pratica. Repor um
     * fragmento de copia e copiar um fragmento; repor um fragmento de paridade e ler o
     * codigo inteiro. A diferenca e o fator k, e ela vira trafego de rede todo dia.
     */
    int bytesLidosParaReconstruirUm();
}
