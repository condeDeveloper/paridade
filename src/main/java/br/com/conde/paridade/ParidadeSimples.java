package br.com.conde.paridade;

/**
 * A PARIDADE SIMPLES: k fragmentos de dado e um de ou-exclusivo.
 *
 * <p>E o esquema do disco em conjunto de nivel cinco e e o primeiro que qualquer um inventa
 * sozinho. O fragmento extra e o ou-exclusivo de todos os outros, e recuperar um perdido e
 * o mesmo ou-exclusivo de novo, porque somar duas vezes a mesma coisa desfaz.
 *
 * <p>Ele e o caso particular de Reed-Solomon com um fragmento de paridade, e esta aqui
 * separado por um motivo pratico e nao didatico: ele nao precisa de corpo finito nenhum, so
 * do ou-exclusivo que todo processador tem. Num sistema de verdade isso e ordens de
 * grandeza mais rapido.
 *
 * <h2>Duas perdas</h2>
 *
 * <p>Com duas perdas ele recusa, e o teste exige que ele RECUSE e nao que ele erre. Nao
 * existe conta que recupere: duas incognitas, uma equacao. O importante e que ele sabe
 * disso, porque num apagamento se sabe quem sumiu.
 */
public final class ParidadeSimples implements Esquema {

    private final int dados;
    private final int bytesPorFragmento;

    public ParidadeSimples(int dados, int bytesPorFragmento) {
        if (dados < 1) throw new IllegalArgumentException("pelo menos um fragmento de dado");
        if (bytesPorFragmento < 1) throw new IllegalArgumentException("pelo menos um byte");

        this.dados = dados;
        this.bytesPorFragmento = bytesPorFragmento;
    }

    @Override
    public String nome() {
        return "paridade " + dados + "+1";
    }

    @Override
    public int fragmentos() {
        return dados + 1;
    }

    @Override
    public int bytesDeDado() {
        return dados * bytesPorFragmento;
    }

    @Override
    public int bytesGuardados() {
        return (dados + 1) * bytesPorFragmento;
    }

    @Override
    public byte[][] codificar(byte[] entrada) {
        if (entrada.length != bytesDeDado())
            throw new IllegalArgumentException(
                    "este esquema guarda " + bytesDeDado() + " bytes e recebeu " + entrada.length);

        byte[][] saida = new byte[dados + 1][bytesPorFragmento];

        for (int i = 0; i < dados; i++)
            System.arraycopy(entrada, i * bytesPorFragmento, saida[i], 0, bytesPorFragmento);

        for (int i = 0; i < dados; i++)
            for (int t = 0; t < bytesPorFragmento; t++) saida[dados][t] ^= saida[i][t];

        return saida;
    }

    @Override
    public byte[] decodificar(byte[][] fragmentos) {
        int perdidos = 0;
        int qualPerdido = -1;

        for (int i = 0; i < fragmentos.length; i++)
            if (fragmentos[i] == null) {
                perdidos++;
                qualPerdido = i;
            }

        if (perdidos > 1) return null;

        byte[][] completo = fragmentos.clone();

        if (perdidos == 1) {
            byte[] reposto = new byte[bytesPorFragmento];

            for (int i = 0; i < fragmentos.length; i++) {
                if (i == qualPerdido) continue;

                for (int t = 0; t < bytesPorFragmento; t++) reposto[t] ^= fragmentos[i][t];
            }

            completo[qualPerdido] = reposto;
        }

        byte[] saida = new byte[bytesDeDado()];

        for (int i = 0; i < dados; i++)
            System.arraycopy(completo[i], 0, saida, i * bytesPorFragmento, bytesPorFragmento);

        return saida;
    }

    @Override
    public int bytesLidosParaReconstruirUm() {
        /* Para repor um fragmento e preciso ler todos os outros. */
        return dados * bytesPorFragmento;
    }
}
