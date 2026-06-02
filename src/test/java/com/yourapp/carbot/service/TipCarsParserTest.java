package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TipCarsParserTest {

    private final TipCarsParser parser = new TipCarsParser();

    @Test
    void resolvesBodyTypesFromTipCarsTitleAndUrl() throws Exception {
        assertThat(extractCarType(
                "Hyundai i30 START PLUS 1.5i 80kW CZ DPH",
                "",
                "https://www.tipcars.com/hyundai-i30/kombi/benzin/hyundai-i30-start-plus.html"))
                .isEqualTo("WAGON");

        assertThat(extractCarType(
                "Skoda Fabia Combi 1.0 TSI Ambition",
                "",
                "https://www.tipcars.com/skoda-fabia/kombi/benzin/skoda-fabia-combi.html"))
                .isEqualTo("WAGON");

        assertThat(extractCarType(
                "Renault Captur techno mildhybrid 140 EDC MY25",
                "",
                "https://www.tipcars.com/renault-captur/hatchback/hybridni-benzin/renault-captur-techno.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Ford Puma Titanium, 5dverova, 1.0 EcoBoost",
                "",
                "https://www.tipcars.com/ford-puma/hatchback/benzin/ford-puma.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Ford Tourneo Courier Active, Tourneo, 1.0 EcoBoost",
                "",
                "https://www.tipcars.com/ford-tourneo-courier/osobni/benzin/ford-tourneo-courier.html"))
                .isEqualTo("MINIVAN");

        assertThat(extractCarType(
                "Suzuki Jimny 1.3i 63KW KLIMA 4X4 TAŽNÉ",
                "",
                "https://www.tipcars.com/suzuki-jimny/terenni/benzin/suzuki-jimny.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Citroën Berlingo 1,5 BlueHDi DPH 1.maj původ ČR",
                "",
                "https://www.tipcars.com/citroen-berlingo/kombi/nafta/citroen-berlingo.html"))
                .isEqualTo("MINIVAN");

        assertThat(extractCarType(
                "Ford C-MAX 1,5 EcoBoost 1.majitel, pěkný",
                "",
                "https://www.tipcars.com/ford-c-max/kombi/benzin/ford-c-max.html"))
                .isEqualTo("MINIVAN");

        assertThat(extractCarType(
                "Audi A5 2.0i 140KW AUT KAMERY SERVISKA",
                "",
                "https://www.tipcars.com/audi-a5/limuzina/benzin/audi-a5-2-0i-140kw-aut-kamery-serviska.html"))
                .isEqualTo("SEDAN");

        assertThat(extractCarType(
                "Tesla Model 3 Long Range 4WD 74kWh, SoH 89%",
                "",
                "https://www.tipcars.com/tesla-model-3/sedan/elektro/tesla-model-3-long-range-4wd-74kwh-soh-89.html"))
                .isEqualTo("SEDAN");

        assertThat(extractCarType(
                "Nissan Pathfinder 2.5 dCi, 4X4, 7 mist",
                "",
                "https://www.tipcars.com/nissan-pathfinder/terenni/nafta/nissan-pathfinder-2-5-dci-4x4-7-mist.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Renault ZOE",
                "",
                "https://www.tipcars.com/renault-zoe/elektro/renault-zoe.html"))
                .isEqualTo("HATCHBACK");
    }

    @Test
    void buildsCurrentTipCarsPaginationUrls() throws Exception {
        assertThat(buildPageUrl(1)).isEqualTo("https://www.tipcars.com/osobni/");
        assertThat(buildPageUrl(2)).isEqualTo("https://www.tipcars.com/?str=2-20");
        assertThat(buildPageUrl(5)).isEqualTo("https://www.tipcars.com/?str=5-20");
    }

    @Test
    void resolvesHybridFuelFromTipCarsTitlesBeforePetrolUrl() throws Exception {
        assertThat(extractFuelType("Toyota C-HR 1.8 Hybrid, Automat"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Jeep Renegade 1.5 Turbo e-Hybrid, Automat"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Hyundai Tucson 1.6 T-GDI PHEV"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Volvo XC60 2,0 B5 Aut. AWD CZ Dark Plus"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("BMW Rada 3 3.0D 150kW M PAKET SERVIS. KN."))
                .isEqualTo("DIESEL");
    }

    private String extractCarType(String title, String text, String url) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractCarType", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text, url);
    }

    private String extractFuelType(String text) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractFuelType", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, text);
    }

    private String buildPageUrl(int page) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("buildPageUrl", int.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, page);
    }
}
