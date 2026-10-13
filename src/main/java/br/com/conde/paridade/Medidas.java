package br.com.conde.paridade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AS CINCO TABELAS que o medidor imprime e que os testes conferem numero por numero.
 *
 * <p>Todo numero daqui sai de tentar TODOS os padroes de perda que existem para cada
 * esquema. Nenhum vem de formula, nenhum vem de amostra, nenhum vem de relogio de maquina.
 * Os mesmos numeros saem em qualquer sistema.
 */
public final class Medidas {

    public static final long SEMENTE = 20_261_013L;

    /** Bytes por fragmento. Pequeno porque o que custa e a varredura, nao o tamanho. */
    public static final int BYTES = 16;

    private Medidas() {}

    /** Os esquemas comparados, todos com no maximo catorze fragmentos. */
    public static List<Esquema> esquemas() {
        return List.of(
                new Replicacao(2, BYTES),
                new Replicacao(3, BYTES),
                new Replicacao(5, BYTES),
                new ParidadeSimples(4, BYTES),
                new ParidadeSimples(8, BYTES),
                new ReedSolomon(4, 2, BYTES),
                new ReedSolomon(6, 3, BYTES),
                new ReedSolomon(8, 2, BYTES),
                new ReedSolomon(10, 4, BYTES));
    }

    private static final Map<String, Juiz.Veredito> VEREDITOS = new LinkedHashMap<>();

    /** O veredito do juiz para um esquema, calculado uma vez so. */
    public static synchronized Juiz.Veredito veredito(Esquema esquema) {
        return VEREDITOS.computeIfAbsent(esquema.nome(), nome -> {
            byte[] dados = new Sorteio(SEMENTE).bytes(esquema.bytesDeDado());

            return Juiz.julgar(esquema, dados);
        });
    }

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String esquema, int fragmentos, long padroes, long recuperados,
            long recusados, long errados, int toleraSempre) {}

    /** Todo padrao de perda recupera o byte certo ou recusa. Nenhum devolve dado errado. */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> saida = new ArrayList<>();

        for (Esquema esquema : esquemas()) {
            Juiz.Veredito v = veredito(esquema);

            saida.add(new LinhaCorrecao(esquema.nome(), v.fragmentos(), v.padroes(),
                    v.recuperados(), v.recusados(), v.errados(), v.toleraSempre()));
        }

        return saida;
    }

    // ───────────────────── 2. o espaco ─────────────────────

    public record LinhaEspaco(String esquema, int fragmentos, double espacoPorByte,
            int toleraSempre, double espacoPorPerdaTolerada,
            double fracaoDePadroesQueSobrevive) {}

    /** Quanto se guarda por byte de dado, e quantas perdas isso compra. */
    public static List<LinhaEspaco> oEspaco() {
        List<LinhaEspaco> saida = new ArrayList<>();

        for (Esquema esquema : esquemas()) {
            Juiz.Veredito v = veredito(esquema);

            double porPerda = v.toleraSempre() == 0
                    ? Double.POSITIVE_INFINITY
                    : esquema.espacoPorByte() / v.toleraSempre();

            saida.add(new LinhaEspaco(esquema.nome(), esquema.fragmentos(),
                    esquema.espacoPorByte(), v.toleraSempre(), porPerda,
                    v.fracaoDePadroesQueSobrevive()));
        }

        return saida;
    }

    // ───────────────────── 3. a durabilidade ─────────────────────

    /** As chances de um fragmento sumir, da mais pessimista para a mais otimista. */
    public static final double[] CHANCES = {0.1, 0.01, 0.001};

    public record LinhaDurabilidade(String esquema, double espacoPorByte, int toleraSempre,
            double chanceDoFragmento, double chanceDePerder, double noves) {}

    /** A durabilidade exata, somada sobre os padroes que o juiz reprovou. */
    public static List<LinhaDurabilidade> aDurabilidade() {
        List<LinhaDurabilidade> saida = new ArrayList<>();

        for (double p : CHANCES)
            for (Esquema esquema : esquemas()) {
                Juiz.Veredito v = veredito(esquema);

                double chance = Durabilidade.chanceDePerder(v, p);

                saida.add(new LinhaDurabilidade(esquema.nome(), esquema.espacoPorByte(),
                        v.toleraSempre(), p, chance, Durabilidade.noves(chance)));
            }

        return saida;
    }

    // ───────────────────── 4. a reconstrucao ─────────────────────

    public record LinhaReconstrucao(String esquema, int bytesDoFragmento, int bytesLidos,
            double amplificacao, double espacoPorByte) {}

    /** Quantos bytes e preciso ler para repor um fragmento perdido. */
    public static List<LinhaReconstrucao> aReconstrucao() {
        List<LinhaReconstrucao> saida = new ArrayList<>();

        for (Esquema esquema : esquemas()) {
            int doFragmento = esquema.bytesGuardados() / esquema.fragmentos();

            saida.add(new LinhaReconstrucao(esquema.nome(), doFragmento,
                    esquema.bytesLidosParaReconstruirUm(),
                    (double) esquema.bytesLidosParaReconstruirUm() / doFragmento,
                    esquema.espacoPorByte()));
        }

        return saida;
    }

    // ───────────────────── 5. a largura ─────────────────────

    public record LinhaLargura(int dados, int paridades, int fragmentos, double espacoPorByte,
            int toleraSempre, double chanceDePerder, double noves, double amplificacao) {}

    /** O mesmo codigo ficando mais largo: o espaco cai, o trafego sobe, e a durabilidade? */
    public static List<LinhaLargura> aLargura() {
        List<LinhaLargura> saida = new ArrayList<>();

        for (int paridades : new int[] {2, 4})
            for (int dados : new int[] {2, 4, 6, 8, 10}) {
                if (dados + paridades > 14) continue;

                ReedSolomon esquema = new ReedSolomon(dados, paridades, BYTES);

                Juiz.Veredito v = veredito(esquema);

                double chance = Durabilidade.chanceDePerder(v, 0.01);

                saida.add(new LinhaLargura(dados, paridades, esquema.fragmentos(),
                        esquema.espacoPorByte(), v.toleraSempre(), chance,
                        Durabilidade.noves(chance),
                        (double) esquema.bytesLidosParaReconstruirUm() / BYTES));
            }

        return saida;
    }
}
