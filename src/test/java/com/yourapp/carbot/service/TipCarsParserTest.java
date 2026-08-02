package com.yourapp.carbot.service;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

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
        assertThat(extractCarType(
                "Volkswagen Taigo 1.0 TSI 7DSG People",
                "",
                "https://www.tipcars.com/volkswagen-taigo/kombi/benzin/volkswagen-taigo-1-0-tsi-7dsg-people-9340197.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Hyundai ix20 1.4 VVTi, klimatizace, tazne",
                "",
                "https://www.tipcars.com/hyundai-ix20/hatchback/benzin/hyundai-ix20-1-4-vvti-klimatizace-tazne.html"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType(
                "Peugeot Rifter 1,5 BlueHDI 96kW CZ 1.Maj GT",
                "",
                "https://www.tipcars.com/peugeot-rifter/van/nafta/peugeot-rifter-1-5-bluehdi-96kw-cz-1-maj-gt.html"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType(
                "Skoda Karoq 2.0 TDI 110 kW DSG 4x4 Top Sel",
                "",
                "https://www.tipcars.com/skoda-karoq/kombi/nafta/skoda-karoq-2-0-tdi-110-kw-dsg-4x4-top-sel-9521099.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Dacia Duster Journey hybrid 155",
                "",
                "https://www.tipcars.com/dacia-duster/hatchback/hybridni-benzin/dacia-duster-journey-hybrid-155-59181955.html"))
                .isEqualTo("SUV");
        assertThat(extractCarType(
                "Toyota ProAce Verso 2.0 D-4D 130kW L1 Family Tazne",
                "",
                "https://www.tipcars.com/toyota-proace-verso/kombi/nafta/toyota-proace-verso-2-0-d-4d-130kw-l1-family-tazne-52775826.html"))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType(
                "Opel Vivaro 2.5 CDTI Tour Cosmo",
                "",
                "https://www.tipcars.com/opel-vivaro/kombi/nafta/opel-vivaro-2-5-cdti-tour-cosmo-18578407.html"))
                .isEqualTo("MINIVAN");
    }

    @Test
    void buildsCurrentTipCarsPaginationUrls() throws Exception {
        assertThat(buildPageUrl(1)).isEqualTo("https://www.tipcars.com/osobni/");
        assertThat(buildPageUrl(2)).isEqualTo("https://www.tipcars.com/?str=2-20");
        assertThat(buildPageUrl(5)).isEqualTo("https://www.tipcars.com/?str=5-20");
        assertThat(extractYear("Zalozeno 2012", "Ford Mustang Rok vyroby 1966, Ford Mustang"))
                .isEqualTo(1966);
        assertThat(extractYear("Prvni registrace 2017", "Peugeot 2008 1.2 PureTech"))
                .isEqualTo(2017);
    }

    @Test
    void extractsListCardDataForForbiddenDetailFallback() throws Exception {
        Map<?, ?> listings = extractListListings("""
                <html><body>
                  <article class="vehicle-card">
                    <a href="https://www.tipcars.com/dacia-duster/suv/benzin/dacia-duster-1-6i-6618504.html">
                      <h2>Dacia Duster 1.6i</h2>
                    </a>
                    <img src="/images/duster.jpg">
                    <span>199 900 Kč</span>
                    <span>2018</span>
                    <span>82 000 km</span>
                    <span>Brno</span>
                  </article>
                </body></html>
                """);

        Object listing = listings.get("https://www.tipcars.com/dacia-duster/suv/benzin/dacia-duster-1-6i-6618504.html");

        assertThat(listing).isNotNull();
        assertThat(recordValue(listing, "title")).isEqualTo("Dacia Duster 1.6i");
        assertThat(recordValue(listing, "text")).asString().contains("199 900 Kč");
    }

    @Test
    void extractsListFallbackPriceWithoutJoiningPowerOrMileage() throws Exception {
        assertThat(extractFirstPrice("Fiat Bravo Fiat Bravo 1.4 16V 90 59 000 Kč"))
                .isEqualTo(59_000);
        assertThat(extractFirstPrice("Audi A5 2010 310 300 km 140 000 Kč"))
                .isEqualTo(140_000);
        assertThat(cleanupListTitle("BMW Řada 3 399 000 Kč"))
                .isEqualTo("BMW Řada 3");
        assertThat(cleanupListTitle("Tesla Model 3 Long Range AWD | AMD Ryzen 598 000 Kč 494 215 Kč bez DPH"))
                .isEqualTo("Tesla Model 3 Long Range AWD | AMD Ryzen");
    }

    @Test
    void extractsListFallbackPriceAfterDriveMarker() throws Exception {
        assertThat(extractFirstPrice("Skoda Kodiaq 2.0 TDI 4x4 450 000 Kc"))
                .isEqualTo(450_000);
        assertThat(cleanupListTitle("Skoda Kodiaq 2.0 TDI 4x4 450 000 Kc"))
                .isEqualTo("Skoda Kodiaq 2.0 TDI 4x4");
        assertThat(extractFirstPrice("Audi e-tron 55 300kw SB BlackEdit ZARUKA"))
                .isNull();
        assertThat(cleanupListTitle("Audi e-tron 55 300kw SB BlackEdit ZARUKA"))
                .isEqualTo("Audi e-tron 55 300kw SB BlackEdit ZARUKA");
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
        assertThat(extractFuelType("Hyundai Tucson 1.6 T-GDI HEV"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Toyota Corolla 1,8 HEV, Comfort Tech"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Toyota Yaris Cross 1.5 HEV CVT (2x4) Style"))
                .isEqualTo("HYBRID");
        assertThat(extractFuelType("Skoda Superb iV 1.5 TSI 150 kW DSG Sportlin"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Lexus RX 400h 400 h, 4X4, Automat, CR,1.maj"))
                .isEqualTo("HYBRID");
    }

    @Test
    void resolvesAutomaticTransmissionWithPunctuation() throws Exception {
        assertThat(extractTransmission("Kia Niro Hybrid, Automat, Serv.kniha"))
                .isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("BMW X3 xDrive30d, 4X4, Automat, Kuze"))
                .isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Skoda Scala 1.0 TSI, Automat, CR"))
                .isEqualTo("AUTOMATIC");
    }

    @Test
    void rejectsTipCarsDetailsWhenTitleBrandConflictsWithUrlBrand() throws Exception {
        assertThat(looksTitleUrlBrandMismatch(
                "Toyota Aygo 1.0 VVT-i, Serv.kniha",
                "https://www.tipcars.com/citroen-c4/hatchback/benzin/citroen-c4-feel-1-2-puretech-cr-1-maj-54001459.html"))
                .isTrue();
        assertThat(looksTitleUrlBrandMismatch(
                "Citroen C4 Feel 1.2 PureTech CR 1.maj",
                "https://www.tipcars.com/citroen-c4/hatchback/benzin/citroen-c4-feel-1-2-puretech-cr-1-maj-54001459.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "Mercedes-Benz Tridy E E 300 de 4MATIC",
                "https://www.tipcars.com/mercedes-benz-tridy-e/sedan/hybridni-benzin/mercedes-benz-tridy-e.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "Chery Tiggo 8 UNIQUE FWD",
                "https://www.tipcars.com/chery-tiggo-8/suv/benzin/chery-tiggo-8-unique-fwd-22537404.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "Chery Tiggo 7 HEV",
                "https://www.tipcars.com/chery-tiggo-7/suv/hybridni-benzin/chery-tiggo-7-hev-22537403.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "SsangYong Korando Clever 1.5 T-GDI",
                "https://www.tipcars.com/ssangyong-korando/suv/benzin/ssangyong-korando-clever-1-5-t-gdi-54000280.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "Xpeng G9 AWD Performance - 575 HP !!!",
                "https://www.tipcars.com/xpeng-g9/suv/elektro/xpeng-g9-awd-performance-575-hp-56710015.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "MG ZS EXCITE",
                "https://www.tipcars.com/mg-zs/suv/benzin/mg-zs-excite-59181956.html"))
                .isFalse();
        assertThat(looksTitleUrlBrandMismatch(
                "DS Automobiles DS7 Crossback PERF. LINE + E-TENSE 360K 4x4",
                "https://www.tipcars.com/ds-automobiles-ds7-crossback/suv/hybridni-benzin/ds-automobiles-ds7-crossback-perf-line-e-tense-360k-4x4-50919028.html"))
                .isFalse();
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

    @Test
    void repairsTipCarsMojibakeAfterWhitespaceNormalizationFromLogs() throws Exception {
        assertThat(repairMojibake("\u0139 koda Superb 1,5TSI 110KW STYLE odp.DPH"))
                .isEqualTo("\u0160koda Superb 1,5TSI 110KW STYLE odp.DPH");
        assertThat(repairMojibake("BMW \u0139\u0098ada 3 BMW E90 320d"))
                .isEqualTo("BMW \u0158ada 3 BMW E90 320d");
        assertThat(repairMojibake("Hyundai i30 1.0 T-GDI Comfort automat"))
                .isEqualTo("Hyundai i30 1.0 T-GDI Comfort automat");
        assertThat(repairMojibake("Kol\u0102\u00ADn"))
                .isEqualTo("Kol\u00EDn");
        assertThat(repairMojibake("Ford Galaxy 2.0TDCi MANU\u0102\u0081L 7M\u0102\u0164ST TA\u0139\u00BDN\u0102\u2030"))
                .isEqualTo("Ford Galaxy 2.0TDCi MANU\u00C1L 7M\u00CDST TA\u017DN\u00C9");
        assertThat(repairMojibake("Mazda 3 2.0i AUTOMAT LED KAMERA V\u0102\u0165H\u0139\u0098EV"))
                .isEqualTo("Mazda 3 2.0i AUTOMAT LED KAMERA V\u00DDH\u0158EV");
        assertThat(repairMojibake("Ford Mustang GT 5.0 Premium /AT/k\u0139\u017B\u0139\u013Ee/440 PS"))
                .isEqualTo("Ford Mustang GT 5.0 Premium /AT/k\u016F\u017Ee/440 PS");
    }

    @Test
    void repairsCurrentTipCarsMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("Tesla Model 3 SR+ Facelift, TaĹľnĂ©, DPH"))
                .isEqualTo("Tesla Model 3 SR+ Facelift, Tažné, DPH");
        assertThat(repairMojibake("Seat Ibiza 1.2 12V, ÄŚR,1.maj, Serv.kniha"))
                .isEqualTo("Seat Ibiza 1.2 12V, ČR,1.maj, Serv.kniha");
        assertThat(repairMojibake("Ĺ koda Fabia Ambition 1.0 TSI, ÄŚR,1.maj"))
                .isEqualTo("Škoda Fabia Ambition 1.0 TSI, ČR,1.maj");
        assertThat(repairMojibake("Tesla Model 3 SR+ Facelift, Ta\u0139\u013En\u0102\u00A9, DPH"))
                .isEqualTo("Tesla Model 3 SR+ Facelift, Ta\u017En\u00E9, DPH");
        assertThat(repairMojibake("Ford Fiesta 1.3, 2.maj,\u00C4\u015AR"))
                .isEqualTo("Ford Fiesta 1.3, 2.maj,\u010CR");
        assertThat(repairMojibake("Honda CR-V Executive nejvy\u0139\u02C7\u0139\u02C7\u0102\u00AD v\u0102\u02DDbava"))
                .isEqualTo("Honda CR-V Executive nejvy\u0161\u0161\u00ED v\u00FDbava");
        assertThat(repairMojibake("\u0139\u00A0koda Superb Style 2.0 TDI, Automat, K\u0139\u017B\u0139\u013Ee"))
                .isEqualTo("\u0160koda Superb Style 2.0 TDI, Automat, K\u016F\u017Ee");
        assertThat(repairMojibake("Ford Transit 2.2,85kW,KLIM.,9M\u0102\u0164ST,TA\u0139\u02DD.ZA\u0139\u0098."))
                .isEqualTo("Ford Transit 2.2,85kW,KLIM.,9M\u00CDST,TA\u017D.ZA\u0158.");
        assertThat(repairMojibake("Suzuki Swift 1.2i KOUPENO \u00C4\u015AR,1.MAJITEL"))
                .isEqualTo("Suzuki Swift 1.2i KOUPENO \u010CR,1.MAJITEL");
        assertThat(repairMojibake("\u0139\u00A0koda Rapid 1.2 Tsi, Monte Carlo"))
                .isEqualTo("\u0160koda Rapid 1.2 Tsi, Monte Carlo");
        assertThat(repairMojibake("Toyota Yaris 1.5i, \u00C4\u015AR-1m, ComTech, z\u0102\u02C7ruka"))
                .isEqualTo("Toyota Yaris 1.5i, \u010CR-1m, ComTech, z\u00E1ruka");
        assertThat(repairMojibake("Ford Tourneo Connect 1.5TDCi MAXi 7m\u0102\u00ADst, TITANIUM"))
                .isEqualTo("Ford Tourneo Connect 1.5TDCi MAXi 7m\u00EDst, TITANIUM");
        assertThat(repairMojibake("Mercedes-Benz T\u0139\u2122\u0102\u00ADdy E 220d 4MATIC, KEYLESS,DISTRONIC"))
                .isEqualTo("Mercedes-Benz T\u0159\u00EDdy E 220d 4MATIC, KEYLESS,DISTRONIC");
        assertThat(repairMojibake("Volkswagen Tayron 2.0TDI DSG 4MOTION LIFE 7 M\u0102\u0164ST"))
                .isEqualTo("Volkswagen Tayron 2.0TDI DSG 4MOTION LIFE 7 M\u00CDST");
        assertThat(repairMojibake("Peugeot 5008 2.0HDI 110KW man\u00E2\u20AC\u201C 2x PNEU"))
                .isEqualTo("Peugeot 5008 2.0HDI 110KW man\u2013 2x PNEU");
        assertThat(repairMojibake("Citro\u0102\u00ABn Berlingo SHINE XL 1.5BHDi 96kW CZ DPH"))
                .isEqualTo("Citro\u00EBn Berlingo SHINE XL 1.5BHDi 96kW CZ DPH");
    }

    @Test
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

    private String extractTransmission(String text) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractTransmission", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, text);
    }

    private boolean looksCommercialOrCamperListing(String title, String url, String text) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("looksCommercialOrCamperListing", String.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, url, text);
    }

    private boolean looksTitleUrlBrandMismatch(String title, String url) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("looksTitleUrlBrandMismatch", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, url);
    }

    private String buildPageUrl(int page) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("buildPageUrl", int.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, page);
    }

    private Map<?, ?> extractListListings(String html) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractListListings", org.jsoup.nodes.Document.class);
        method.setAccessible(true);
        return (Map<?, ?>) method.invoke(parser, Jsoup.parse(html, "https://www.tipcars.com/osobni/"));
    }

    private Integer extractFirstPrice(String text) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractFirstPrice", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, text);
    }

    private String cleanupListTitle(String title) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("cleanupListTitle", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title);
    }

    private Object recordValue(Object record, String accessor) throws Exception {
        Method method = record.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return method.invoke(record);
    }

    private Integer extractYear(String text, String title) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractYear", String.class, String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, text, title);
    }

    private String repairMojibake(String value) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("repairMojibake", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
    }
}
