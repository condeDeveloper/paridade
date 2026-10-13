package br.com.conde.paridade;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("espaco") || comando.equals("tudo")) espaco();
        if (comando.equals("durabilidade") || comando.equals("tudo")) durabilidade();
        if (comando.equals("reconstrucao") || comando.equals("tudo")) reconstrucao();
        if (comando.equals("largura") || comando.equals("tudo")) largura();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODO PADRAO DE PERDA RECUPERA O BYTE CERTO OU RECUSA?");
        System.out.println("O juiz apaga todo subconjunto de fragmentos e compara byte por byte.");
        System.out.println();

        var linhas = Medidas.aCorrecao();

        System.out.printf("  %-16s %6s %10s %12s %12s %10s %14s%n",
                "esquema", "frag", "padroes", "recuperou", "recusou", "ERROU", "tolera sempre");

        long padroes = 0;
        long errados = 0;

        for (var l : linhas) {
            System.out.printf("  %-16s %6d %10d %12d %12d %10d %14d%n",
                    l.esquema(), l.fragmentos(), l.padroes(), l.recuperados(), l.recusados(),
                    l.errados(), l.toleraSempre());

            padroes += l.padroes();
            errados += l.errados();
        }

        System.out.println();
        System.out.printf("  %d padroes de perda tentados ao todo, %d com dado errado.%n",
                padroes, errados);
        System.out.println();
    }

    private static void espaco() {
        System.out.println();
        System.out.println("ESPACO GASTO POR PERDA TOLERADA.");
        System.out.println();

        System.out.printf("  %-16s %6s %12s %14s %18s %18s%n",
                "esquema", "frag", "espaco", "tolera sempre", "espaco por perda",
                "padroes que vive");

        for (var l : Medidas.oEspaco())
            System.out.printf(Locale.ROOT, "  %-16s %6d %12.4f %14d %18.4f %18.4f%n",
                    l.esquema(), l.fragmentos(), l.espacoPorByte(), l.toleraSempre(),
                    l.espacoPorPerdaTolerada(), l.fracaoDePadroesQueSobrevive());

        System.out.println();
    }

    private static void durabilidade() {
        System.out.println();
        System.out.println("A DURABILIDADE EXATA, somada sobre os padroes que o juiz reprovou.");
        System.out.println("Supondo que os fragmentos somem de forma independente.");
        System.out.println();

        System.out.printf("  %12s %-16s %10s %8s %16s %10s%n",
                "chance frag", "esquema", "espaco", "tolera", "chance de perder", "noves");

        for (var l : Medidas.aDurabilidade())
            System.out.printf(Locale.ROOT, "  %12.3f %-16s %10.4f %8d %16.3e %10.2f%n",
                    l.chanceDoFragmento(), l.esquema(), l.espacoPorByte(), l.toleraSempre(),
                    l.chanceDePerder(), l.noves());

        System.out.println();
    }

    private static void reconstrucao() {
        System.out.println();
        System.out.println("O QUE CUSTA REPOR UM FRAGMENTO PERDIDO.");
        System.out.println();

        System.out.printf("  %-16s %14s %14s %16s %12s%n",
                "esquema", "frag em bytes", "bytes lidos", "amplificacao", "espaco");

        for (var l : Medidas.aReconstrucao())
            System.out.printf(Locale.ROOT, "  %-16s %14d %14d %16.2f %12.4f%n",
                    l.esquema(), l.bytesDoFragmento(), l.bytesLidos(), l.amplificacao(),
                    l.espacoPorByte());

        System.out.println();
    }

    private static void largura() {
        System.out.println();
        System.out.println("A LARGURA DO CODIGO MUDA OS TRES EIXOS, com chance de 0,01 por fragmento.");
        System.out.println();

        System.out.printf("  %8s %10s %8s %10s %8s %16s %10s %14s%n",
                "dados", "paridades", "frag", "espaco", "tolera", "chance de perder",
                "noves", "amplificacao");

        for (var l : Medidas.aLargura())
            System.out.printf(Locale.ROOT, "  %8d %10d %8d %10.4f %8d %16.3e %10.2f %14.2f%n",
                    l.dados(), l.paridades(), l.fragmentos(), l.espacoPorByte(),
                    l.toleraSempre(), l.chanceDePerder(), l.noves(), l.amplificacao());

        System.out.println();
    }
}
