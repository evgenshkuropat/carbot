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

        assertThat(extractCarType(
                "Peugeot 2008 1.2 Puretech",
                "",
                "https://www.tipcars.com/peugeot-2008/hatchback/benzin/peugeot-2008-1-2-puretech.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Porsche Macan 3.0i AWD PDK7",
                "",
                "https://www.tipcars.com/porsche-macan/hatchback/benzin/porsche-macan-3-0i-awd-pdk7-55577322.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Seat Ateca",
                "",
                "https://www.tipcars.com/seat-ateca/hatchback/benzin/seat-ateca-100168344.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Skoda Kamiq 1.5 TSI 110 kW Style DSG",
                "",
                "https://www.tipcars.com/skoda-kamiq/hatchback/benzin/skoda-kamiq-1-5-tsi-110-kw-style-dsg.html"))
                .isEqualTo("SUV");

        assertThat(extractCarType(
                "Volkswagen Golf Sportsvan 1,6 TDI BMT, WEBASTO",
                "",
                "https://www.tipcars.com/volkswagen-golf-sportsvan/mpv/nafta/volkswagen-golf-sportsvan-1-6-tdi-bmt-webasto.html"))
                .isEqualTo("MINIVAN");

        assertThat(extractCarType(
                "Fiat Scudo 2,0 JTD 110CV L",
                "",
                "https://www.tipcars.com/fiat-scudo/kombi/nafta/fiat-scudo-2-0-jtd-110cv-l.html"))
                .isEqualTo("MINIVAN");

        assertThat(extractCarType(
                "Skoda Enyaq iV 80 150kW TZ DPH SOH 91%",
                "",
                "https://www.tipcars.com/skoda-enyaq/kombi/elektro/skoda-enyaq.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Volkswagen Multivan 4MOTION DSG HIGHLINE",
                "",
                "https://www.tipcars.com/volkswagen-multivan/kombi/nafta/volkswagen-multivan.html"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType(
                "Kia XCeed 1,6 T-GDi 7DCT",
                "",
                "https://www.tipcars.com/kia-xceed/cuv/benzin/kia-xceed.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Citroen C3 Aircross 1.2i",
                "",
                "https://www.tipcars.com/citroen-c3-aircross/hatchback/benzin/citroen-c3-aircross.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Peugeot 5008 1.2i 96kW",
                "",
                "https://www.tipcars.com/peugeot-5008/kombi/benzin/peugeot-5008.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Volkswagen Tiguan Allspace 1,5 TSi",
                "",
                "https://www.tipcars.com/volkswagen-tiguan-allspace/kombi/benzin/volkswagen-tiguan-allspace.html"))
                .isEqualTo("SUV");
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
        assertThat(extractFuelType("Volvo XC40 2,0 B3 Black Edition"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Volvo V60 0,0 B4 Plus Dark Plus Dark"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Skoda Octavia 1.5 TSI e-tec"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Citroen C8 2.0, LPG, 8 mist, Tazne, Klima"))
                .isEqualTo("LPG");
        assertThat(extractFuelType("Mercedes-Benz GLC 300 de 4M AMG"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Volkswagen Touareg eHybrid 3.0 V6 TSI"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Dacia Duster 1,0 TCe ECO-G Expression"))
                .isEqualTo("LPG");
        assertThat(extractFuelType("BMW Rada 3 3.0D 150kW M PAKET SERVIS. KN."))
                .isEqualTo("DIESEL");
        assertThat(extractFuelType("Peugeot 2008 Active 1.2 PureTech"))
                .isEqualTo("PETROL");
        assertThat(extractFuelType("Citroen C3 Aircross Feel 1.2 PureTech, Serv.kniha"))
                .isEqualTo("PETROL");
        assertThat(extractFuelType("Peugeot 3008 Hybrid 145 PureTech"))
                .isEqualTo("HYBRID");
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

    @Test
    void repairsFreshTipCarsMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("\u0139\u00A0koda Fabia 1,2HTP Klimatizace"))
                .isEqualTo("\u0160koda Fabia 1,2HTP Klimatizace");
        assertThat(repairMojibake("Opel Crossland X 1.2T AUTOMAT NOV\u0102\u2030 ROZVODY"))
                .isEqualTo("Opel Crossland X 1.2T AUTOMAT NOV\u00C9 ROZVODY");
        assertThat(repairMojibake("Audi A6 3.0 TDI, 210kw, ta\u0139\u013En\u0102\u00A9"))
                .isEqualTo("Audi A6 3.0 TDI, 210kw, ta\u017En\u00E9");
        assertThat(repairMojibake("Renault M\u0102\u00A9gane 1.5DCi INTENS automat"))
                .isEqualTo("Renault M\u00E9gane 1.5DCi INTENS automat");
        assertThat(repairMojibake("BMW \u0139\u0098ada 5 540d xDrive Touring"))
                .isEqualTo("BMW \u0158ada 5 540d xDrive Touring");
        assertThat(repairMojibake("Mercedes-Benz T\u0139\u2122\u0102\u00ADdy A A 200 Progressive linie"))
                .isEqualTo("Mercedes-Benz T\u0159\u00EDdy A A 200 Progressive linie");
        assertThat(repairMojibake("Zl\u0102\u00ADn"))
                .isEqualTo("Zl\u00EDn");
        assertThat(repairMojibake("Peugeot 208 1.5 HDI 75 KW,1.MAJ,\u00C4\u015AR,DPH..."))
                .isEqualTo("Peugeot 208 1.5 HDI 75 KW,1.MAJ,\u010CR,DPH...");
    }

    void repairsTipCarsMojibakeBeforeOutputLegacy() throws Exception {
        assertThat(repairMojibake("Ĺ koda Superb 2,0 TDi Ambition"))
                .isEqualTo("Škoda Superb 2,0 TDi Ambition");
        assertThat(repairMojibake("Mazda CX-7 2.2i DISI TURBO AWD, ZĂVÄšS"))
                .isEqualTo("Mazda CX-7 2.2i DISI TURBO AWD, ZÁVĚS");
        assertThat(repairMojibake("Ford Focus 1.6i, 2.maj,ÄŚR"))
                .isEqualTo("Ford Focus 1.6i, 2.maj,ČR");
        assertThat(repairMojibake("Hyundai Tucson NovĂ˝ Comfort 1,6 T-GDi"))
                .isEqualTo("Hyundai Tucson Nový Comfort 1,6 T-GDi");
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

    private String repairMojibake(String value) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("repairMojibake", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
    }
}
