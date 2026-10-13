package br.com.conde.paridade;

/**
 * O JUIZ: TODO padrao de perda que existe, e a comparacao byte por byte.
 *
 * <p>Ele e o juiz certo e nao so o conveniente por tres motivos.
 *
 * <p>Primeiro, ele e exaustivo de verdade. Com n fragmentos ha 2 elevado a n conjuntos de
 * perda, e ele tenta os 2 elevado a n. Nao ha sorteio, nao ha amostra, nao ha "os casos
 * interessantes": um codigo que falha num padrao esquisito falha aqui.
 *
 * <p>Segundo, ele nao sabe nada sobre o esquema. Nao usa a matriz, nao usa k nem m, nao
 * confia em nenhuma propriedade alegada. Ele apaga fragmentos, pede o dado de volta e
 * compara com o original.
 *
 * <p>Terceiro, e o que mais importa, ele separa RECUSAR de ERRAR. Um esquema que devolve
 * null porque perdeu demais esta certo; um que devolve bytes que nao sao o dado esta
 * errado de um jeito que nenhuma medida de espaco ou de velocidade pegaria, e que num
 * sistema de verdade significa um arquivo corrompido em silencio.
 *
 * <p>E ele exige CORRECAO e nao eficiencia. Quanto espaco o esquema gasta, quantos bytes
 * ele le para reconstruir, quanto ele dura: nada disso entra no julgamento. Essas sao as
 * tabelas, e elas so querem dizer alguma coisa depois que o esquema esta certo.
 */
public final class Juiz {

    private Juiz() {}

    /**
     * O resultado de tentar todos os padroes de perda.
     *
     * @param recuperadosPorPeso quantos padroes de cada numero de perdas foram recuperados
     * @param totalPorPeso quantos padroes de cada numero de perdas existem
     * @param toleraSempre o maior j tal que TODO padrao de j perdas recupera
     */
    public record Veredito(String esquema, int fragmentos, long padroes, long recuperados,
            long recusados, long errados, long[] recuperadosPorPeso, long[] totalPorPeso,
            int toleraSempre) {

        public boolean certo() {
            return errados == 0;
        }

        /** Quantos padroes de perda, de todos, o esquema sobrevive. */
        public double fracaoDePadroesQueSobrevive() {
            return (double) recuperados / padroes;
        }
    }

    /**
     * Apaga todo subconjunto de fragmentos e confere o que volta.
     *
     * <p>O laco de fora anda de 0 a 2 elevado a n menos 1, e o bit i ligado quer dizer que
     * o fragmento i sumiu. Com catorze fragmentos sao 16.384 tentativas de recuperacao.
     */
    public static Veredito julgar(Esquema esquema, byte[] dados) {
        int n = esquema.fragmentos();

        if (n > 24)
            throw new IllegalArgumentException(
                    "2 elevado a " + n + " padroes nao e uma varredura, e uma espera");

        byte[][] original = esquema.codificar(dados);

        long recuperados = 0;
        long recusados = 0;
        long errados = 0;

        long[] recuperadosPorPeso = new long[n + 1];
        long[] totalPorPeso = new long[n + 1];

        int padroes = 1 << n;

        for (int perda = 0; perda < padroes; perda++) {
            int peso = Integer.bitCount(perda);

            totalPorPeso[peso]++;

            byte[][] sobrou = new byte[n][];

            for (int i = 0; i < n; i++)
                if ((perda & (1 << i)) == 0) sobrou[i] = original[i].clone();

            byte[] voltou = esquema.decodificar(sobrou);

            if (voltou == null) {
                recusados++;
                continue;
            }

            if (!java.util.Arrays.equals(voltou, dados)) {
                errados++;
                continue;
            }

            recuperados++;
            recuperadosPorPeso[peso]++;
        }

        int toleraSempre = 0;

        while (toleraSempre + 1 <= n
                && recuperadosPorPeso[toleraSempre + 1] == totalPorPeso[toleraSempre + 1])
            toleraSempre++;

        return new Veredito(esquema.nome(), n, padroes, recuperados, recusados, errados,
                recuperadosPorPeso, totalPorPeso, toleraSempre);
    }

    /**
     * Qual padrao de perda o esquema errou, se errou algum. Para a mensagem do teste.
     *
     * <p>Devolve -1 quando nao errou nenhum.
     */
    public static int primeiroPadraoErrado(Esquema esquema, byte[] dados) {
        int n = esquema.fragmentos();

        byte[][] original = esquema.codificar(dados);

        for (int perda = 0; perda < (1 << n); perda++) {
            byte[][] sobrou = new byte[n][];

            for (int i = 0; i < n; i++)
                if ((perda & (1 << i)) == 0) sobrou[i] = original[i].clone();

            byte[] voltou = esquema.decodificar(sobrou);

            if (voltou != null && !java.util.Arrays.equals(voltou, dados)) return perda;
        }

        return -1;
    }
}
