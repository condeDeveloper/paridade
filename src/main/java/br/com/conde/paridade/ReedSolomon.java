package br.com.conde.paridade;

/**
 * REED-SOLOMON: k fragmentos de dado, m de paridade, e QUAISQUER k bastam.
 *
 * <p>Essa ultima frase e tudo. Nao "quaisquer k se forem os certos", nao "k com alta
 * probabilidade": quaisquer k dos k mais m fragmentos reconstroem o dado exato. Um codigo
 * com essa propriedade chama-se separavel de distancia maxima, e ela e o limite teorico:
 * nenhum codigo recupera o dado com menos de k fragmentos, porque k fragmentos sao a
 * quantidade de informacao que ha.
 *
 * <h2>Como ela sai da matriz</h2>
 *
 * <p>Codificar e multiplicar os k fragmentos de dado por uma matriz de k mais m linhas por
 * k colunas. Cada linha produz um fragmento. Sobrando k fragmentos quaisquer, pegam-se as
 * k linhas correspondentes, inverte-se, e multiplica-se: volta o dado.
 *
 * <p>Entao "quaisquer k bastam" e literalmente "qualquer escolha de k linhas da uma matriz
 * inversivel", e e a matriz de Vandermonde que garante isso.
 *
 * <h2>Por que a matriz comeca com a identidade</h2>
 *
 * <p>A Vandermonde pura funcionaria, mas os k primeiros fragmentos nao seriam o dado: seriam
 * combinacoes dele. Ler um arquivo sem perda nenhuma exigiria decodificar.
 *
 * <p>Multiplicando a Vandermonde pela inversa das k primeiras linhas dela, o bloco de cima
 * vira a identidade e os k primeiros fragmentos passam a ser o dado em si. Isso se chama
 * forma sistematica, nao muda nada do que o codigo aguenta, e e o que torna a leitura
 * normal de graca.
 */
public final class ReedSolomon implements Esquema {

    private final int dados;
    private final int paridades;
    private final int bytesPorFragmento;

    private final Matriz codificadora;

    public ReedSolomon(int dados, int paridades, int bytesPorFragmento) {
        if (dados < 1) throw new IllegalArgumentException("pelo menos um fragmento de dado");
        if (paridades < 1) throw new IllegalArgumentException("pelo menos um de paridade");
        if (bytesPorFragmento < 1) throw new IllegalArgumentException("pelo menos um byte");

        if (dados + paridades > 256)
            throw new IllegalArgumentException(
                    "sobre bytes nao cabem mais que 256 fragmentos, pediram "
                            + (dados + paridades));

        this.dados = dados;
        this.paridades = paridades;
        this.bytesPorFragmento = bytesPorFragmento;

        Matriz bruta = Matriz.vandermonde(dados + paridades, dados);

        int[] primeiras = new int[dados];
        for (int i = 0; i < dados; i++) primeiras[i] = i;

        Matriz inversaDoTopo = bruta.linhas(primeiras).inversa();

        if (inversaDoTopo == null)
            throw new IllegalStateException("as primeiras linhas da Vandermonde nao inverteram");

        this.codificadora = bruta.vezes(inversaDoTopo);
    }

    @Override
    public String nome() {
        return "RS " + dados + "+" + paridades;
    }

    public int dados() {
        return dados;
    }

    public int paridades() {
        return paridades;
    }

    public Matriz codificadora() {
        return codificadora;
    }

    @Override
    public int fragmentos() {
        return dados + paridades;
    }

    @Override
    public int bytesDeDado() {
        return dados * bytesPorFragmento;
    }

    @Override
    public int bytesGuardados() {
        return (dados + paridades) * bytesPorFragmento;
    }

    @Override
    public byte[][] codificar(byte[] entrada) {
        if (entrada.length != bytesDeDado())
            throw new IllegalArgumentException(
                    "este esquema guarda " + bytesDeDado() + " bytes e recebeu " + entrada.length);

        byte[][] saida = new byte[fragmentos()][bytesPorFragmento];

        for (int linha = 0; linha < fragmentos(); linha++)
            for (int t = 0; t < bytesPorFragmento; t++) {
                int soma = 0;

                for (int coluna = 0; coluna < dados; coluna++)
                    soma = Campo.soma(soma, Campo.produto(codificadora.em(linha, coluna),
                            entrada[coluna * bytesPorFragmento + t] & 0xFF));

                saida[linha][t] = (byte) soma;
            }

        return saida;
    }

    @Override
    public byte[] decodificar(byte[][] fragmentos) {
        int[] sobreviventes = new int[dados];
        int quantos = 0;

        for (int i = 0; i < fragmentos.length && quantos < dados; i++)
            if (fragmentos[i] != null) sobreviventes[quantos++] = i;

        /* Menos de k fragmentos e menos informacao do que o dado tem. Nao ha o que tentar. */
        if (quantos < dados) return null;

        Matriz inversa = codificadora.linhas(sobreviventes).inversa();

        /* Com a Vandermonde isto nunca acontece, e o teste exige que nunca aconteca. */
        if (inversa == null) return null;

        byte[] saida = new byte[bytesDeDado()];

        for (int linha = 0; linha < dados; linha++)
            for (int t = 0; t < bytesPorFragmento; t++) {
                int soma = 0;

                for (int coluna = 0; coluna < dados; coluna++)
                    soma = Campo.soma(soma, Campo.produto(inversa.em(linha, coluna),
                            fragmentos[sobreviventes[coluna]][t] & 0xFF));

                saida[linha * bytesPorFragmento + t] = (byte) soma;
            }

        return saida;
    }

    @Override
    public int bytesLidosParaReconstruirUm() {
        /* Repor um fragmento exige decodificar: k fragmentos lidos para repor um. */
        return dados * bytesPorFragmento;
    }
}
