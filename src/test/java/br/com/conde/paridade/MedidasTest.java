package br.com.conde.paridade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /* Cada tabela varre todos os padroes de perda de cada esquema: uma vez so basta. */
    private static final List<Medidas.LinhaCorrecao> CORRECAO = Medidas.aCorrecao();
    private static final List<Medidas.LinhaEspaco> ESPACO = Medidas.oEspaco();
    private static final List<Medidas.LinhaDurabilidade> DURABILIDADE = Medidas.aDurabilidade();
    private static final List<Medidas.LinhaReconstrucao> RECONSTRUCAO = Medidas.aReconstrucao();
    private static final List<Medidas.LinhaLargura> LARGURA = Medidas.aLargura();

    // ───────────────────── 1. a correcao ─────────────────────

    /**
     * NENHUM ESQUEMA DEVOLVE DADO ERRADO, em padrao de perda nenhum.
     *
     * <p>Sao 18.572 padroes ao todo, e cada um e uma recuperacao tentada e comparada byte
     * por byte. Recusar esta certo; errar nao.
     */
    @Test
    @DisplayName("nos 18.572 padroes de perda, nenhum esquema devolveu dado errado")
    void nenhumErra() {
        assertThat(CORRECAO).hasSize(9);

        assertThat(CORRECAO).allSatisfy(l ->
                assertThat(l.errados()).as(l.esquema()).isZero());

        assertThat(CORRECAO.stream().mapToLong(Medidas.LinhaCorrecao::padroes).sum())
                .isEqualTo(18_572);

        assertThat(CORRECAO).allSatisfy(l ->
                assertThat(l.recuperados() + l.recusados())
                        .as(l.esquema() + ": todo padrao recupera ou recusa")
                        .isEqualTo(l.padroes()));
    }

    /**
     * O QUE CADA ESQUEMA TOLERA, MEDIDO E NAO ALEGADO.
     *
     * <p>A coluna sai do juiz: e o maior j tal que TODOS os padroes de j perdas recuperam.
     * Reed-Solomon com m paridades tolera exatamente m, que e o teorema, conferido.
     */
    @Test
    @DisplayName("Reed-Solomon tolera exatamente o numero de paridades, e isso foi medido")
    void oQueCadaUmTolera() {
        assertThat(correcao("RS 4+2").toleraSempre()).isEqualTo(2);
        assertThat(correcao("RS 6+3").toleraSempre()).isEqualTo(3);
        assertThat(correcao("RS 10+4").toleraSempre()).isEqualTo(4);

        assertThat(correcao("paridade 4+1").toleraSempre())
                .as("a paridade simples tolera uma, e so uma")
                .isEqualTo(1);

        assertThat(correcao("copias 3").toleraSempre())
                .as("tres copias toleram duas perdas")
                .isEqualTo(2);

        assertThat(correcao("copias 5").toleraSempre()).isEqualTo(4);
    }

    private static Medidas.LinhaCorrecao correcao(String esquema) {
        return CORRECAO.stream()
                .filter(l -> l.esquema().equals(esquema))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 2. o espaco ─────────────────────

    @Test
    @DisplayName("RS 4+2 tolera as mesmas duas perdas que tres copias, com metade do espaco")
    void aMesmaToleranciaPorMetadeDoEspaco() {
        assertThat(espaco("copias 3").espacoPorByte()).isEqualTo(3.0);
        assertThat(espaco("RS 4+2").espacoPorByte()).isEqualTo(1.5);

        assertThat(espaco("RS 4+2").toleraSempre())
                .isEqualTo(espaco("copias 3").toleraSempre());

        assertThat(espaco("RS 10+4").espacoPorByte())
                .as("e RS 10+4 tolera o dobro gastando menos da metade")
                .isEqualTo(1.4);

        assertThat(espaco("RS 10+4").espacoPorPerdaTolerada()).isCloseTo(0.35, within(0.0001));
        assertThat(espaco("copias 3").espacoPorPerdaTolerada()).isCloseTo(1.5, within(0.0001));
    }

    private static Medidas.LinhaEspaco espaco(String esquema) {
        return ESPACO.stream()
                .filter(l -> l.esquema().equals(esquema))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. a durabilidade ─────────────────────

    /**
     * A CORRECAO: tolerar as mesmas perdas nao e ter a mesma durabilidade.
     *
     * <p>RS 4+2 tolera duas perdas e tres copias tambem. Com chance de 1% por fragmento, a
     * chance de perder o dado e 1,955e-05 contra 1,000e-06: vinte vezes PIOR, apesar de
     * tolerar o mesmo. Sao seis coisas que podem falhar em vez de tres.
     */
    @Test
    @DisplayName("tolerando as mesmas duas perdas, RS 4+2 e 20 vezes menos duravel que 3 copias")
    void aMesmaToleranciaNaoEhAMesmaDurabilidade() {
        double doRs = durabilidade("RS 4+2", 0.01).chanceDePerder();
        double dasCopias = durabilidade("copias 3", 0.01).chanceDePerder();

        assertThat(dasCopias).isCloseTo(1.0e-6, within(1e-9));
        assertThat(doRs).isCloseTo(1.955e-5, within(1e-8));

        assertThat(doRs / dasCopias)
                .as("vinte vezes pior, tolerando exatamente a mesma coisa")
                .isCloseTo(19.55, within(0.01));

        assertThat(durabilidade("RS 4+2", 0.01).toleraSempre())
                .isEqualTo(durabilidade("copias 3", 0.01).toleraSempre());

        assertThat(durabilidade("RS 8+2", 0.01).chanceDePerder() / dasCopias)
                .as("e quanto mais fino o codigo, pior: RS 8+2 tolera as mesmas duas e e 113 vezes pior")
                .isCloseTo(113.8, within(0.1));

        assertThat(durabilidade("RS 8+2", 0.01).toleraSempre()).isEqualTo(2);
    }

    /**
     * E A VOLTA POR CIMA: com paridade suficiente, Reed-Solomon ganha e ganha feio.
     *
     * <p>RS 10+4 gasta 1,4 vezes o dado contra 3,0 de tres copias, e ainda e cinco vezes
     * mais duravel. Nao ha truque: sao quatro paridades em vez de duas.
     */
    @Test
    @DisplayName("RS 10+4 e 5 vezes mais duravel que 3 copias gastando menos da metade")
    void comParidadeSuficienteReedSolomonGanha() {
        double doRs = durabilidade("RS 10+4", 0.01).chanceDePerder();
        double dasCopias = durabilidade("copias 3", 0.01).chanceDePerder();

        assertThat(doRs).isCloseTo(1.857e-7, within(1e-10));

        assertThat(dasCopias / doRs).isCloseTo(5.38, within(0.01));

        assertThat(espaco("RS 10+4").espacoPorByte())
                .isLessThan(espaco("copias 3").espacoPorByte() / 2);
    }

    /**
     * E NO MESMO ESPACO EXATO, a diferenca e de tres ordens de grandeza.
     *
     * <p>RS 2+4 gasta 3,0 vezes o dado, igual a tres copias. A chance de perder cai de
     * 1,000e-06 para 5,950e-10.
     */
    @Test
    @DisplayName("com o mesmo espaco de 3 copias, RS 2+4 e 1.680 vezes mais duravel")
    void noMesmoEspacoADiferencaEhDeTresOrdens() {
        Medidas.LinhaLargura rs = largura(2, 4);

        assertThat(rs.espacoPorByte()).isEqualTo(3.0);
        assertThat(rs.chanceDePerder()).isCloseTo(5.950e-10, within(1e-13));

        assertThat(durabilidade("copias 3", 0.01).chanceDePerder() / rs.chanceDePerder())
                .isCloseTo(1_680.0, within(1.0));
    }

    /**
     * A CORRECAO SEGUNDA: contar padroes de perda aponta para o lado contrario.
     *
     * <p>Tres copias sobrevivem a 87,5% de todos os padroes de perda; RS 10+4 sobrevive a
     * 9,0%. E RS 10+4 e cinco vezes mais duravel. Quase toda a probabilidade mora nos
     * padroes de poucas perdas, e la o RS ganha.
     */
    @Test
    @DisplayName("3 copias sobrevivem a 87% dos padroes e RS 10+4 a 9%, e o RS e mais duravel")
    void contarPadroesApontaProLadoErrado() {
        assertThat(espaco("copias 3").fracaoDePadroesQueSobrevive()).isCloseTo(0.875, within(1e-6));
        assertThat(espaco("RS 10+4").fracaoDePadroesQueSobrevive()).isCloseTo(0.0898, within(1e-4));

        assertThat(espaco("copias 3").fracaoDePadroesQueSobrevive())
                .isGreaterThan(espaco("RS 10+4").fracaoDePadroesQueSobrevive() * 9);

        assertThat(durabilidade("RS 10+4", 0.01).chanceDePerder())
                .as("e mesmo assim o RS perde o dado menos vezes")
                .isLessThan(durabilidade("copias 3", 0.01).chanceDePerder());

        assertThat(espaco("paridade 8+1").fracaoDePadroesQueSobrevive())
                .as("a paridade 8+1 sobrevive a menos de 2% dos padroes")
                .isCloseTo(0.0195, within(1e-4));
    }

    @Test
    @DisplayName("com c copias a chance de perder e p elevado a c, exatamente")
    void aReplicacaoEhPElevadoAC() {
        for (double p : Medidas.CHANCES)
            for (int copias : new int[] {2, 3, 5})
                assertThat(durabilidade("copias " + copias, p).chanceDePerder())
                        .as("copias " + copias + " com p " + p)
                        .isCloseTo(Math.pow(p, copias), within(Math.pow(p, copias) * 1e-9));
    }

    private static Medidas.LinhaDurabilidade durabilidade(String esquema, double p) {
        return DURABILIDADE.stream()
                .filter(l -> l.esquema().equals(esquema) && l.chanceDoFragmento() == p)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. a reconstrucao ─────────────────────

    /**
     * O QUE A TABELA DE ESPACO NAO MOSTRA: repor um fragmento custa k vezes o que ele vale.
     *
     * <p>Repor uma copia e copiar uma copia. Repor um fragmento de RS 10+4 e ler dez
     * fragmentos. Isso vira trafego de rede todo dia, e e a razao pratica de a replicacao
     * continuar viva.
     */
    @Test
    @DisplayName("repor um fragmento de RS 10+4 le 10 vezes o fragmento, e de copia le 1")
    void reporCustaKVezes() {
        assertThat(reconstrucao("copias 3").amplificacao()).isEqualTo(1.0);
        assertThat(reconstrucao("copias 5").amplificacao()).isEqualTo(1.0);

        assertThat(reconstrucao("RS 10+4").amplificacao()).isEqualTo(10.0);
        assertThat(reconstrucao("RS 4+2").amplificacao()).isEqualTo(4.0);
        assertThat(reconstrucao("paridade 8+1").amplificacao()).isEqualTo(8.0);

        assertThat(RECONSTRUCAO).filteredOn(l -> l.esquema().startsWith("copias"))
                .allSatisfy(l -> assertThat(l.amplificacao()).as(l.esquema()).isEqualTo(1.0));
    }

    private static Medidas.LinhaReconstrucao reconstrucao(String esquema) {
        return RECONSTRUCAO.stream()
                .filter(l -> l.esquema().equals(esquema))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. a largura ─────────────────────

    /**
     * ALARGAR O CODIGO MEXE NOS TRES EIXOS, e dois deles pioram.
     *
     * <p>Com duas paridades, ir de dois para dez fragmentos de dado derruba o espaco de 2,0
     * para 1,2, derruba a durabilidade de 5,40 para 3,69 noves, e multiplica por cinco o
     * trafego de reconstrucao. So um dos tres melhora.
     */
    @Test
    @DisplayName("alargar o codigo economiza espaco e custa durabilidade e trafego")
    void alargarPioraDoisDosTres() {
        assertThat(largura(2, 2).espacoPorByte()).isEqualTo(2.0);
        assertThat(largura(10, 2).espacoPorByte()).isEqualTo(1.2);

        assertThat(largura(2, 2).noves()).isCloseTo(5.40, within(0.01));
        assertThat(largura(10, 2).noves()).isCloseTo(3.69, within(0.01));

        assertThat(largura(2, 2).amplificacao()).isEqualTo(2.0);
        assertThat(largura(10, 2).amplificacao()).isEqualTo(10.0);

        for (int paridades : new int[] {2, 4}) {
            double novesAnterior = Double.MAX_VALUE;

            for (int dados : new int[] {2, 4, 6, 8, 10}) {
                Medidas.LinhaLargura linha = largura(dados, paridades);

                assertThat(linha.noves())
                        .as("k " + dados + " m " + paridades + ": alargar sempre piora a durabilidade")
                        .isLessThan(novesAnterior);

                novesAnterior = linha.noves();
            }
        }
    }

    @Test
    @DisplayName("o numero de paridades decide a durabilidade muito mais que a largura")
    void aParidadeDecideMaisQueALargura() {
        assertThat(largura(10, 4).noves())
                .as("o codigo mais largo com quatro paridades ganha do mais estreito com duas")
                .isGreaterThan(largura(2, 2).noves());

        assertThat(largura(10, 4).espacoPorByte())
                .as("e ainda gasta menos espaco que ele")
                .isLessThan(largura(2, 2).espacoPorByte());

        assertThat(largura(10, 4).noves()).isCloseTo(6.73, within(0.01));
        assertThat(largura(2, 2).noves()).isCloseTo(5.40, within(0.01));
    }

    private static Medidas.LinhaLargura largura(int dados, int paridades) {
        return LARGURA.stream()
                .filter(l -> l.dados() == dados && l.paridades() == paridades)
                .findFirst()
                .orElseThrow();
    }
}
