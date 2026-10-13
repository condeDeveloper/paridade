package br.com.conde.paridade;

/**
 * A DURABILIDADE: a chance de o dado sumir, somada padrao por padrao.
 *
 * <p>Ela NAO vem de formula. Ela vem da tabela do juiz: para cada padrao de perda que o
 * juiz constatou que o esquema nao recupera, soma-se a probabilidade daquele padrao
 * acontecer. Nada aqui supoe que o codigo tolera o que ele diz que tolera.
 *
 * <p>Isso importa mais do que parece. A conta de sempre e "tolera m perdas, entao a chance
 * de perder e a cauda da binomial a partir de m mais um". Essa conta esta certa para um
 * codigo separavel de distancia maxima e ERRADA para qualquer outro, inclusive para a
 * replicacao, que recupera de montes de padroes de peso alto. Somar sobre os padroes que o
 * juiz reprovou vale para os dois.
 *
 * <h2>A hipotese que fica de fora</h2>
 *
 * <p>Esta conta supoe que os fragmentos somem de forma INDEPENDENTE. Num sistema de verdade
 * eles nao somem: um disco, um bastidor, uma fonte, uma atualizacao de software derrubam
 * varios de uma vez. A independencia e a hipotese mais generosa possivel com codigos de
 * muitos fragmentos, e e por isso que os numeros daqui favorecem Reed-Solomon mais do que a
 * realidade favoreceria.
 */
public final class Durabilidade {

    private Durabilidade() {}

    /**
     * A chance de nao dar para recuperar o dado, com cada fragmento sumindo com chance p.
     *
     * <p>Soma, sobre todo numero de perdas j, a quantidade de padroes de peso j que o juiz
     * reprovou vezes a chance de exatamente aqueles j sumirem.
     */
    public static double chanceDePerder(Juiz.Veredito veredito, double p) {
        if (p < 0 || p > 1) throw new IllegalArgumentException("p e uma chance, entre zero e um");

        double total = 0;

        int n = veredito.fragmentos();

        for (int j = 0; j <= n; j++) {
            long reprovados = veredito.totalPorPeso()[j] - veredito.recuperadosPorPeso()[j];

            if (reprovados == 0) continue;

            total += reprovados * Math.pow(p, j) * Math.pow(1 - p, n - j);
        }

        return total;
    }

    /**
     * Quantos noves de durabilidade, que e como a industria fala disso.
     *
     * <p>Onze noves quer dizer chance de perder de um em cem bilhoes.
     */
    public static double noves(double chanceDePerder) {
        if (chanceDePerder <= 0) return Double.POSITIVE_INFINITY;

        return -Math.log10(chanceDePerder);
    }
}
