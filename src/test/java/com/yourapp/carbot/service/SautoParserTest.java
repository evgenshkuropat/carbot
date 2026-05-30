package com.yourapp.carbot.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SautoParserTest {

    private final SautoParser parser = new SautoParser();

    @Test
    void resolvesBodyTypesFromFreshSautoLogs() throws Exception {
        assertThat(extractCarType("Kia K4 CL4 1,6 T-GDi GPF 7DCT TOP", "", "https://www.sauto.cz/osobni/detail/kia/k4/209567385"))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Honda City 1.3 I", "", "https://www.sauto.cz/osobni/detail/honda/city/210217619"))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Peugeot 307 PEUGEOT 307cc roku 2004", "", "https://www.sauto.cz/osobni/detail/peugeot/307/210508373"))
                .isEqualTo("CABRIO");
        assertThat(extractCarType("Renault Thalia Thalia 1,4 , 55 kW, garazovany", "", "https://www.sauto.cz/osobni/detail/renault/thalia/210498536"))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Peugeot Partner 1.9 D", "kombi", "https://www.sauto.cz/osobni/detail/peugeot/partner/209441238"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Chrysler Grand Voyager 2,4 LPG", "kombi", "https://www.sauto.cz/osobni/detail/chrysler/grand-voyager/210405968"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Renault Scenic 1.5 dCi", "kombi", "https://www.sauto.cz/osobni/detail/renault/scenic/210047736"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat Grande Punto Fiat Grande Punto 2006 1.3 JTD", "kombi", "https://www.sauto.cz/osobni/detail/fiat/grande-punto/210196538"))
                .isEqualTo("HATCHBACK");
        assertThat(extractCarType("Volkswagen Golf Volkswagen Golf 2008 nova stk", "suv", "https://www.sauto.cz/osobni/detail/volkswagen/golf/210497417"))
                .isEqualTo("HATCHBACK");
        assertThat(extractCarType("Hyundai Matrix hyundai matrix 1.6i", "kombi", "https://www.sauto.cz/osobni/detail/hyundai/matrix/210470000"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Hyundai Accent 1.6i MPI, Serv.kniha, Tazne", "", "https://www.sauto.cz/osobni/detail/hyundai/accent/210380264"))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Ford Mondeo 2.0 TDCi, Klima", "", "https://www.sauto.cz/osobni/detail/ford/mondeo/210346946"))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Daewoo Nubira 1.6i,STK7/27", "", "https://www.sauto.cz/osobni/detail/daewoo/nubira/210319271"))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Volkswagen Golf Plus VW GOLF PLUS 1,9", "", "https://www.sauto.cz/osobni/detail/volkswagen/golf-plus/210122711"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Peugeot 206 1.1 i, NOVA CENA, po STK", "kombi", "https://www.sauto.cz/osobni/detail/peugeot/206/210229829"))
                .isEqualTo("HATCHBACK");
    }

    @Test
    void keepsNormalSalePriceWhenDealerMentionsZeroPercentFinancing() throws Exception {
        Document doc = Jsoup.parse("""
                <html><body>
                    <div class="price">30 000 Kc Zobrazit vice o cene Poznamka k cene:
                    Moznost Vaseho vozu protiuctem. Financovani je pro Vas zajisteno
                    od 0% akontace, splatky dle Vasich moznosti.</div>
                </body></html>
                """);

        assertThat(extractPriceValueDirect(doc, "Skoda Fabia 1.6 74 KW", "")).isEqualTo(30_000);
    }

    private String extractCarType(String title, String text, String url) throws Exception {
        Method method = SautoParser.class.getDeclaredMethod("extractCarType", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text, url);
    }

    private Integer extractPriceValueDirect(Document doc, String title, String listingText) throws Exception {
        Method method = SautoParser.class.getDeclaredMethod("extractPriceValueDirect", Document.class, String.class, String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, doc, title, listingText);
    }
}
