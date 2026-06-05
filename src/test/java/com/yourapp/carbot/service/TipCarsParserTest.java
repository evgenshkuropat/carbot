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

        assertThat(extractCarType(
                "Ford S-MAX 129 kW, ODPOCET DPH",
                "",
                "https://www.tipcars.com/ford-s-max/hatchback/nafta/ford-s-max-129-kw-odpocet-dph.html"))
                .isEqualTo("MINIVAN");

        assertThat(extractCarType(
                "Skoda Elroq Premium Lodge 85 /82kWH",
                "",
                "https://www.tipcars.com/skoda-elroq/mpv/elektro/skoda-elroq-premium-lodge.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Citroen C4 Picasso 1,2 PureTech 110 MAN",
                "",
                "https://www.tipcars.com/citroen-c4-picasso/hatchback/benzin/citroen-c4-picasso.html"))
                .isEqualTo("MINIVAN");
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
        assertThat(extractFuelType("BMW Rada 2 225xe Active Tourer, 4X4"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("BMW X3 M Sport xDrive30e"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("BMW X1 xDrive25e, CR, 1.MAJ"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Mercedes-Benz Tridy E E 300 e 4MATIC"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Mercedes-Benz GLE 2,0 350 de 4MATIC kupe"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("BMW XM Label"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Jaecoo 7 Exclusive 1.5t GDI SHS"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Volvo XC60 2,0 B5 Aut. AWD CZ Dark Plus"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("BMW Rada 3 3.0D 150kW M PAKET SERVIS. KN."))
                .isEqualTo("DIESEL");
    }

    @Test
    void rejectsFreshCommercialTipCarsStorageCandidates() throws Exception {
        assertThat(looksCommercialOrCamperListing(
                "Opel Movano Van L2 (L) 2.2 CDTi 6 MT",
                "https://www.tipcars.com/opel-movano/nafta/opel-movano-van-l2-l-2-2-cdti-6-mt.html",
                ""))
                .isTrue();
        assertThat(looksCommercialOrCamperListing(
                "Opel Combo L2 (XL) 1.5 CDTi 102k MT6",
                "https://www.tipcars.com/opel-combo/nafta/opel-combo-l2-xl-1-5-cdti.html",
                ""))
                .isTrue();
        assertThat(looksCommercialOrCamperListing(
                "Volkswagen Transporter 2.0Tdi 125kw 4x4 CZ DPH",
                "https://www.tipcars.com/volkswagen-transporter/van/nafta/volkswagen-transporter-2-0tdi.html",
                ""))
                .isTrue();
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

    private boolean looksCommercialOrCamperListing(String title, String url, String text) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("looksCommercialOrCamperListing", String.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, url, text);
    }

    private String buildPageUrl(int page) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("buildPageUrl", int.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, page);
    }
}
