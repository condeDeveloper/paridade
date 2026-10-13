package br.com.conde.paridade;

/**
 * A REPLICACAO: guarde o dado inteiro tantas vezes.
 *
 * <p>E o esquema mais burro que existe e e o mais usado do mundo. Tres copias e o padrao de
 * fato em quase todo sistema de arquivos distribuido, e nao e por ignorancia.
 *
 * <p>O que ela tem e que a codificacao e a decodificacao somem: nao ha matriz, nao ha
 * corpo finito, nao ha nada. Guardar e copiar, ler e ler qualquer uma, e repor uma copia
 * perdida e copiar uma copia. Nenhuma outra linha das tabelas chega perto nesse ponto.
 *
 * <p>O que ela nao tem e espaco: com c copias ela gasta c vezes o dado para tolerar c menos
 * uma perdas. Quem quiser aguentar quatro perdas gasta cinco vezes.
 */
public final class Replicacao implements Esquema {

    private final int copias;
    private final int bytes;

    public Replicacao(int copias, int bytes) {
        if (copias < 1) throw new IllegalArgumentException("pelo menos uma copia");
        if (bytes < 1) throw new IllegalArgumentException("pelo menos um byte");

        this.copias = copias;
        this.bytes = bytes;
    }

    @Override
    public String nome() {
        return "copias " + copias;
    }

    @Override
    public int fragmentos() {
        return copias;
    }

    @Override
    public int bytesDeDado() {
        return bytes;
    }

    @Override
    public int bytesGuardados() {
        return bytes * copias;
    }

    @Override
    public byte[][] codificar(byte[] dados) {
        if (dados.length != bytes)
            throw new IllegalArgumentException(
                    "este esquema guarda " + bytes + " bytes e recebeu " + dados.length);

        byte[][] saida = new byte[copias][];

        for (int i = 0; i < copias; i++) saida[i] = dados.clone();

        return saida;
    }

    @Override
    public byte[] decodificar(byte[][] fragmentos) {
        for (byte[] copia : fragmentos)
            if (copia != null) return copia.clone();

        return null;
    }

    @Override
    public int bytesLidosParaReconstruirUm() {
        /* Repor uma copia e copiar outra copia, e nada mais. E a vantagem inteira dela. */
        return bytes;
    }
}
