package br.com.conde.paridade;

/**
 * UMA MATRIZ NO CORPO DE 256, com o unico metodo que importa: inverter.
 *
 * <p>Codificar e multiplicar os dados por uma matriz. Recuperar e multiplicar o que sobrou
 * pela INVERSA das linhas que sobreviveram. Entao a pergunta "este esquema aguenta perder
 * estes fragmentos" e, literalmente, "estas linhas formam uma matriz inversivel".
 *
 * <p>A inversao e Gauss-Jordan, igual a do colegio, com uma diferenca que facilita: aqui
 * nao existe erro de arredondamento nem pivo quase nulo. Ou o pivo e zero ou ele tem
 * inverso exato. Nao ha decisao numerica a tomar, e e por isso que a resposta deste
 * repositorio e sempre sim ou nao, nunca "quase".
 */
public final class Matriz {

    private final int[][] valores;

    public Matriz(int[][] valores) {
        if (valores.length == 0) throw new IllegalArgumentException("matriz sem linha");

        this.valores = new int[valores.length][];

        for (int i = 0; i < valores.length; i++) {
            if (valores[i].length != valores[0].length)
                throw new IllegalArgumentException("linhas de tamanhos diferentes");

            this.valores[i] = valores[i].clone();
        }
    }

    public int linhas() {
        return valores.length;
    }

    public int colunas() {
        return valores[0].length;
    }

    public int em(int linha, int coluna) {
        return valores[linha][coluna];
    }

    public Matriz vezes(Matriz outra) {
        if (colunas() != outra.linhas())
            throw new IllegalArgumentException(
                    "nao da para multiplicar " + linhas() + "x" + colunas()
                            + " por " + outra.linhas() + "x" + outra.colunas());

        int[][] saida = new int[linhas()][outra.colunas()];

        for (int i = 0; i < linhas(); i++)
            for (int j = 0; j < outra.colunas(); j++) {
                int soma = 0;

                for (int t = 0; t < colunas(); t++)
                    soma = Campo.soma(soma, Campo.produto(valores[i][t], outra.em(t, j)));

                saida[i][j] = soma;
            }

        return new Matriz(saida);
    }

    /** As linhas escolhidas, nessa ordem. E o que sobrou depois das perdas. */
    public Matriz linhas(int[] quais) {
        int[][] saida = new int[quais.length][];

        for (int i = 0; i < quais.length; i++) saida[i] = valores[quais[i]].clone();

        return new Matriz(saida);
    }

    public static Matriz identidade(int tamanho) {
        int[][] saida = new int[tamanho][tamanho];

        for (int i = 0; i < tamanho; i++) saida[i][i] = 1;

        return new Matriz(saida);
    }

    /**
     * A matriz de Vandermonde: a linha i e 1, i, i ao quadrado, e assim por diante.
     *
     * <p>Ela e a escolha certa aqui por uma propriedade e nao por conveniencia: com nos
     * distintos, QUALQUER subconjunto de k linhas dela e inversivel. E isso quer dizer, em
     * palavras do assunto, que quaisquer k fragmentos bastam para recuperar o dado.
     *
     * <p>O corpo tem 256 elementos, entao ha 256 nos distintos e nao mais. Esse e o limite
     * de quantos fragmentos um codigo sobre bytes pode ter.
     */
    public static Matriz vandermonde(int linhas, int colunas) {
        if (linhas > 256)
            throw new IllegalArgumentException(
                    "o corpo tem 256 elementos e so ha 256 nos distintos, pediram " + linhas);

        int[][] saida = new int[linhas][colunas];

        for (int i = 0; i < linhas; i++)
            for (int j = 0; j < colunas; j++) saida[i][j] = Campo.potencia(i, j);

        return new Matriz(saida);
    }

    /** A inversa, ou null quando a matriz e singular. Null aqui quer dizer dado perdido. */
    public Matriz inversa() {
        if (linhas() != colunas()) throw new IllegalArgumentException("so matriz quadrada inverte");

        int n = linhas();

        int[][] esquerda = new int[n][];
        int[][] direita = new int[n][n];

        for (int i = 0; i < n; i++) {
            esquerda[i] = valores[i].clone();
            direita[i][i] = 1;
        }

        for (int coluna = 0; coluna < n; coluna++) {
            int pivo = -1;

            for (int linha = coluna; linha < n; linha++)
                if (esquerda[linha][coluna] != 0) {
                    pivo = linha;
                    break;
                }

            /* Sem pivo nesta coluna a matriz e singular: as linhas que sobraram nao bastam. */
            if (pivo < 0) return null;

            int[] guarda = esquerda[coluna];
            esquerda[coluna] = esquerda[pivo];
            esquerda[pivo] = guarda;

            guarda = direita[coluna];
            direita[coluna] = direita[pivo];
            direita[pivo] = guarda;

            int inverso = Campo.inverso(esquerda[coluna][coluna]);

            for (int j = 0; j < n; j++) {
                esquerda[coluna][j] = Campo.produto(esquerda[coluna][j], inverso);
                direita[coluna][j] = Campo.produto(direita[coluna][j], inverso);
            }

            for (int linha = 0; linha < n; linha++) {
                if (linha == coluna || esquerda[linha][coluna] == 0) continue;

                int fator = esquerda[linha][coluna];

                for (int j = 0; j < n; j++) {
                    esquerda[linha][j] = Campo.soma(esquerda[linha][j],
                            Campo.produto(fator, esquerda[coluna][j]));

                    direita[linha][j] = Campo.soma(direita[linha][j],
                            Campo.produto(fator, direita[coluna][j]));
                }
            }
        }

        return new Matriz(direita);
    }
}
