package br.com.conde.paridade;

/**
 * O SORTEIO DA CASA: um gerador linear congruente, escrito aqui de proposito.
 *
 * <p>Ele existe para que a mesma semente de os mesmos bytes de dado em qualquer maquina e
 * em qualquer versao de Java. A biblioteca padrao nao promete isso entre versoes, e um
 * repositorio cujos numeros estao escritos no README nao pode depender disso.
 *
 * <p>Sao as constantes de Knuth, as mesmas do gerador de 64 bits que todo mundo usa.
 *
 * <h2>Por que o deslocamento de 32</h2>
 *
 * <p>Num gerador linear congruente os bits de baixo tem periodo curto de um jeito exato e
 * nao estatistico: o ultimo bit alterna com periodo dois, os oito ultimos repetem com
 * periodo 256.
 *
 * <p>E aqui isso arruinaria o juiz, que compara BYTES. Sem o deslocamento, um bloco de
 * dado maior que 256 bytes se repete byte por byte, e dois fragmentos diferentes do mesmo
 * bloco ficam IDENTICOS. Com fragmentos de 16 bytes e um dado de 320, os fragmentos 0 e 16
 * carregariam os mesmos bytes.
 *
 * <p>Um decodificador que trocasse dois fragmentos de lugar devolveria o dado errado e
 * passaria na comparacao, porque os bytes bateriam assim mesmo. O juiz deixaria de ser
 * juiz. Ha um teste so sobre isso, e ele conta os pares identicos.
 */
public final class Sorteio {

    private long estado;

    public Sorteio(long semente) {
        this.estado = semente;
    }

    /** Um inteiro de 32 bits, tirado da metade de cima do estado. */
    public int proximo() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;

        return (int) (estado >>> 32);
    }

    /** Um inteiro de 0 inclusive ate o limite exclusive. */
    public int ate(int limite) {
        if (limite < 1) throw new IllegalArgumentException("limite precisa ser positivo");

        return Math.floorMod(proximo(), limite);
    }

    /** Tantos bytes de dado. */
    public byte[] bytes(int quantos) {
        byte[] saida = new byte[quantos];

        for (int i = 0; i < quantos; i++) saida[i] = (byte) proximo();

        return saida;
    }
}
