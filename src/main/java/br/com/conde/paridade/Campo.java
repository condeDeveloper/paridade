package br.com.conde.paridade;

/**
 * O CORPO DE 256 ELEMENTOS, que e a aritmetica em que tudo aqui acontece.
 *
 * <p>Um byte nao pode ser somado como numero: 200 mais 100 nao cabe em oito bits. Entao a
 * codificacao por apagamento troca a aritmetica inteira por outra, em que soma, produto,
 * divisao e inverso existem todos e NUNCA saem de oito bits.
 *
 * <p>Nela um byte e um polinomio com coeficientes zero ou um. Somar e somar coeficiente a
 * coeficiente modulo dois, que e exatamente o ou-exclusivo. Multiplicar e multiplicar
 * polinomios e tirar o resto pelo polinomio irredutivel 0x11D, que e o mesmo que o padrao
 * de disco e o de codigo de barras usam.
 *
 * <h2>O juiz de dentro</h2>
 *
 * <p>O produto rapido usa tabelas de logaritmo, que e o truque de sempre: o produto de a
 * por b vale g elevado a soma dos logaritmos. Tabela e justamente onde um erro de um indice
 * passa despercebido, e por isso ha aqui tambem o produto DEVAGAR, feito do jeito ingenuo,
 * deslocando e somando. Um teste exige que os dois concordem nos 65.536 pares possiveis.
 *
 * <p>Nao e uma amostra: sao todos.
 */
public final class Campo {

    /** x^8 + x^4 + x^3 + x^2 + 1, o mesmo polinomio do padrao de disco. */
    public static final int POLINOMIO = 0x11D;

    /**
     * DOIS, e nao tres.
     *
     * <p>Um gerador precisa ser elemento PRIMITIVO: as potencias dele tem que percorrer os
     * 255 elementos nao nulos antes de voltar ao um. Eu escrevi tres primeiro, porque tres
     * e o gerador do corpo do padrao de cifragem, que usa outro polinomio.
     *
     * <p>Com 0x11D, tres percorre 51 valores e volta. A tabela de logaritmo ficaria com 204
     * posicoes em branco, todo produto que caisse nelas daria zero, e a matriz de
     * Vandermonde nao inverteria. Foi exatamente assim que isto apareceu. Ha um teste so
     * sobre isso.
     */
    public static final int GERADOR = 2;

    private static final int[] EXPOENTE = new int[512];
    private static final int[] LOGARITMO = new int[256];

    static {
        int atual = 1;

        for (int i = 0; i < 255; i++) {
            EXPOENTE[i] = atual;
            LOGARITMO[atual] = i;

            atual = produtoDevagar(atual, GERADOR);
        }

        /* A segunda metade repete a primeira: assim o produto nao precisa de resto. */
        for (int i = 255; i < 512; i++) EXPOENTE[i] = EXPOENTE[i - 255];
    }

    private Campo() {}

    /** Somar e o ou-exclusivo, e e por isso que somar e subtrair sao a mesma coisa. */
    public static int soma(int a, int b) {
        return a ^ b;
    }

    public static int produto(int a, int b) {
        if (a == 0 || b == 0) return 0;

        return EXPOENTE[LOGARITMO[a] + LOGARITMO[b]];
    }

    public static int inverso(int a) {
        if (a == 0)
            throw new ArithmeticException("zero nao tem inverso, aqui como em qualquer lugar");

        return EXPOENTE[255 - LOGARITMO[a]];
    }

    public static int divisao(int a, int b) {
        return produto(a, inverso(b));
    }

    /** O elemento a elevado a n. */
    public static int potencia(int a, int n) {
        int resposta = 1;

        for (int i = 0; i < n; i++) resposta = produto(resposta, a);

        return resposta;
    }

    /**
     * O produto do jeito ingenuo: desloca e soma, tirando o resto quando passa de oito bits.
     *
     * <p>Ele existe para conferir o produto rapido e para montar a tabela sem depender dela.
     */
    public static int produtoDevagar(int a, int b) {
        int resposta = 0;

        while (b != 0) {
            if ((b & 1) != 0) resposta ^= a;

            boolean vaiPassar = (a & 0x80) != 0;

            a = (a << 1) & 0xFF;

            if (vaiPassar) a ^= POLINOMIO & 0xFF;

            b >>= 1;
        }

        return resposta;
    }
}
