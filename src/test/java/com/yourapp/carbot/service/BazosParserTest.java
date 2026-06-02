package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class BazosParserTest {

    private final BazosParser parser = new BazosParser();

    @Test
    void resolvesBrandsFromBazosTitlesBeforeNoisyPageText() throws Exception {
        assertThat(extractBrand("2021 TARRACO XCELLENCE 4x4 FACELIFT-mozna vymena,splatky", "tiguan kodiaq skoda"))
                .isEqualTo("SEAT");
        assertThat(extractBrand("SEAT ALHAMBRA 2.0TDI 103KW 7MIST TAZNE Z.", "volkswagen sharan"))
                .isEqualTo("SEAT");
        assertThat(extractBrand("PRODAM MAZDU 6 GH VE VYBORNEM STAVU", ""))
                .isEqualTo("MAZDA");
        assertThat(extractBrand("ID.4 1.st MAX 80kwh 150KW MATRIX HEAD UP 107tkm m2021", "peugeot 2008"))
                .isEqualTo("VOLKSWAGEN");
        assertThat(extractBrand("Toyata Yaris 1.5 16V, Edice Y20, 41 tis km", ""))
                .isEqualTo("TOYOTA");
    }

    @Test
    void resolvesFuelFromSuzukiBazosTitles() throws Exception {
        assertThat(extractFuelType("Suzuki Vitara 1.6 DDiS AllGrip 4x4")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Suzuki Grand Vitara 2.4 VVT")).isEqualTo("PETROL");
        assertThat(extractFuelType("Suzuki Vitara 1.4 BoosterJet AllGrip Mild-Hybrid r2020")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Suzuki Virara 1.6 Ddis")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Suzuki Alto,1.0i,50kw")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodam Suzuki sx4,1.6")).isEqualTo("PETROL");
    }

    @Test
    void resolvesFuelFromBmwAndAudiBazosTitles() throws Exception {
        assertThat(extractFuelType("Audi S3")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW. 118d")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW 320xd 135kw automat xdrive")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW 530xd E61 LCI M-paket")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW 335i E92 M3 LOOK")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW M3 MANUAL KOMPRESOR")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW Z4 3.0 si MANUAL Coupe")).isEqualTo("PETROL");
        assertThat(extractFuelType("Audi A6 Allroad 235 kW")).isEqualTo("DIESEL");
    }

    @Test
    void resolvesFuelFromMercedesAndMitsubishiBazosTitles() throws Exception {
        assertThat(extractFuelType("Mercedes-Benz E 350 BLUETEC 4MATIC 12/2016")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Mercedes-Benz 126.500 SEC AMG Paket")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodam Mitsubishi Eclipse cross 1,5")).isEqualTo("PETROL");
        assertThat(extractFuelType("Alfa Romeo 156 2.0 JTS 16V Selespeed Distinctive Funny car")).isEqualTo("PETROL");
        assertThat(extractFuelType("Alfa Romeo Stelvio 2.0 Turbo 16V AT8-Q4 Veloce T")).isEqualTo("PETROL");
        assertThat(extractFuelType("Alfa Romeo GTV 2.0 TS 114 kW 1999")).isEqualTo("PETROL");
        assertThat(extractFuelType("Alfa Romeo Giulia VELOCE Q4 280PS, 2.0T, ZF8")).isEqualTo("PETROL");
        assertThat(extractFuelType("Audi A6 Avant 2.0 45 eTFSI 195kW S-tronic")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Honda HR-V 1.5 benzin hybrid")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Honda CR-V 2.0 e:HEV Advance AWD")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Honda CR-V 2.0i-MMD Elegance AWD")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Honda Jazz 1.4 61kw. r.2005")).isEqualTo("PETROL");
        assertThat(extractFuelType("FIAT 500 1.0 11/2022 DPH 61000km zanovni")).isEqualTo("PETROL");
        assertThat(extractFuelType("Fiat 500 / 0.9 TwinAir / SPORT / 77kW / NAVI")).isEqualTo("PETROL");
        assertThat(extractFuelType("Toyota Yaris Cross, 1.5HEV, Adventure, 4x4")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Peugeot 308 SW 1.2 PT 96 kW 130 Allure CZ DPH")).isEqualTo("PETROL");
        assertThat(extractFuelType("PEUGEOT 301 1.2 60kW rok 2016")).isEqualTo("PETROL");
        assertThat(extractFuelType("Citroen C5 combi,2,2 diesel,125 kW, Webasto")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Fiat Freemont 2.0 MJT AT 4x4 125kW")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Fiat Doblo Maxi 2020 1.6 MJT2 dlouha verze")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Mercedes GLC 350 D 4MATIC, 3.0 V6 nez. topeni")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Mercedes E43 AMG")).isEqualTo("PETROL");
        assertThat(extractFuelType("Mitsubishi Outlander 2.4i+HYBRID 4x4 SERVISKA TAZNE")).isEqualTo("HYBRID");
        assertThat(extractFuelType("MITSUBISHI ECLIPSE CROSS 2.4 PHEV 138kW 4x4-12/2022-49.949KM")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Mitsubishi lancer evo")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan Primera 1.8 16V 2006")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan Micra 1.0 IG-T LED KLIMA")).isEqualTo("PETROL");
        assertThat(extractFuelType("Opel Crossland 1.2T 81kW LED LIMITED CARPLAY")).isEqualTo("PETROL");
        assertThat(extractFuelType("Toyota GR86 executive manualni prev. odpocet DPH")).isEqualTo("PETROL");
        assertThat(extractFuelType("Toyota GR Yaris s upravami za skoro 700.000,-")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW e46 330ci, M Packet, 170KW, 231 hp")).isEqualTo("PETROL");
        assertThat(extractFuelType("Mini Cooper")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodam Toyota Mirai Executive")).isEqualTo("ELECTRIC");
        assertThat(extractFuelType("VW Passat B8 Variant GTE 1.4TSI 160kW DSG - zaruka Autodraft")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("VW Golf 8 GTE 1.4 TSI Hybrid 150kW DSG - zaruka Autodraft")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Dacia Lodgy MPV r.2022 1,3benz 96kw 1.majitel")).isEqualTo("PETROL");
        assertThat(extractFuelType("Pekna Dacia Logan MCV 1.2...16V")).isEqualTo("PETROL");
        assertThat(extractFuelType("VOLVO V90 CROSS COUNTRY ULTIMATE B5 173KW 2022 CZ DPH 1MAJ")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Volvo XC 90 B5 AWD INSCRIPTION")).isEqualTo("HYBRID");
    }

    @Test
    void resolvesCarTypesFromBazosTitles() throws Exception {
        assertThat(extractCarType("Seat Leon1.5 TSi 96kW 1majitel CR Xcellence", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Seat Leon ST 1.2 TSI, 81kW, r2017", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Seat Altea XL 1.6 TDI 77 kW Automat", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Seat ibiza", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Suzuki Jimny 1.3 i 2015", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("suzuki jimny 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Samurai 1.3", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Prodam Suzuki sx4,1.6", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Virara 1.6 Ddis", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Alto,1.0i,50kw", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Audi S3", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Alfa Romeo 75 2.0 Twinspark", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mini Cooper 1.5 i 2018 F 56", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("BMW 325i e91", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("BMW F36 430d 258Hp GC 05/2016 original M-Paket", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW 6 GT xDrive M-Paket", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Audi A6 2.0 TDI AVANT Ultra S-tronic 2015", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Audi S6 Quattro UVEDENA CENA BEZ DPH", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW M3 MANUAL KOMPRESOR", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("BMW Z4 3.0 si MANUAL Coupe", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("Citroen C 3 1.5 HDi, Edice Origins Since 1919", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Fiat Dobló 1,6Jtd MAXI klima+5dveri+CR+64000km", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat Talento Kombi 1.6turbo 107kw,novy motor 8mist,zaves", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat Croma 1,9jtd AUTOMAT 2009", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Fiat500", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Fiat 500c 1.2 Lounge 2015", "", "")).isEqualTo("CABRIO");
        assertThat(extractCarType("Fiat Bravo 1,6 JTD, 2008", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Ford Focus Tunier 2014", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("FUSION FACELIFT,1.4 16V 59KW,ROK 2008", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("HONDA CIVIC TOURER 1.6i DTEC 2016 KAMERA", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Astra J Sports Tourer 1.4i Turbo103Kw r.v.10/2015", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Astra K 1,6 CDTI 81kw sport Tourer, Innovation", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Astra K 1,6 CDTI 81kw 2016 ST, Innovation", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Insignia Sport Taurer", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Nissan Primera P12 2.2D", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Nissan Elgrand 3.5 V6", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("OPEL AMPERA PLUGIN-HYBRID ELEKTRO", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Opel Crossland X 1.2i 81kw Inovation", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("OPEL VECTRA C 2.2i 16V EDICE", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Prodame Peugeot Travaller", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Peugeot Traveller 2.0 Blue-HDi Allure L2", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("PEUGEOT 301 1.2 60kW rok 2016", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Corolla ST 1.8 HEV 103kW e-CVT,2024,35tkm", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Toyota Rav 4 2.0D 85Kw Nova STK, 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota Aoris", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Toyota Camry Executive HYBRID", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Yaris Cross, 1.5HEV, Adventure, 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota 4runner - SPECIAL z mise OSN", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota Prius Plus 7mist+LPG 2013", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota Sienna AWD 2017 7 mist 8AT tazne", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota Starlet", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Hyundai SantaFe 4 x 4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Hyundai IX 20", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota Aygo 1.0VVT-i 50kw 4/2013", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Prodám Toyota Mirai Executive", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Škoda Yeti, 2.0 TDi 4X4 Outdoor Nové Rozvody", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("VW ID.3 Pro 150kW IQ.Lights SOH 95,7%", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("VW Passat B8 Varian TDI 110kW DSG", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("VW Arteon SB 2.0 TDI 110kW DSG R-Line", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("VW GOLF PLUS 1,4 TSi 90 KW TOP STAV", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Volvo S80 2.4D5 120 kW Klima Tempomat CR", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Volvo v 90", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Audi A2 1.4 TDI STK 2028", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Renault Talisman 1.6dCI MANUAL VYHREV TAZNE", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Renault Alaskan", "", "")).isEqualTo("PICKUP");
        assertThat(extractCarType("Renault Laguna 2", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Cupra Tavascan Endurance Electric 210kW", "", "")).isEqualTo("SUV");
    }

    @Test
    void resolvesMercedesAndMazdaBodyTypesFromBazosTitles() throws Exception {
        assertThat(extractCarType("MERCEDES BENZ CLA 200 SHOOTING BRAKE AMG", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("E220 All-Terrain 4Matic", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes Benz C 220 CDI T BlueEfficiency (W204)", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes e270cdi rv2000", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes C200CDI, r.v. 2003, 85kW, automat", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes-Benz C180 Kompressor W204 115 kW manual", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes-Benz C 250d 150kW AMG 4MATIC KEYLESS", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes-Benz GLK 320CDI 165KW PANORAMA KAMERA", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Mercedes-Benz EQB 250 Progressive 140kW", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Mercedes E43 AMG", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("MERCEDES-BENZ W246 B180 90kw PRAVIDELNY SERVIS TOP VYBAVA", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Mercedes-Benz 126.500 SEC AMG Paket", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("PRODAM MAZDU 6 GH VE VYBORNEM STAVU", "", "")).isEqualTo("SEDAN");
    }

    @Test
    void resolvesAlfaRomeoTransmissionAndSkipsPartTitles() throws Exception {
        assertThat(extractTransmission("Alfa Romeo 156 2.0 JTS 16V Selespeed Distinctive Funny car")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Alfa Romeo Stelvio 2.0 Turbo 16V AT8-Q4 Veloce T")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Toyota Corolla ST 1.8 HEV 103kW e-CVT,2024,35tkm")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Honda CR-V 2.0 e:HEV Advance AWD")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Honda CR-V 2.0i-MMD Elegance AWD")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Citroen Berlingo 1.5 BlueHDi 130S&S MAN 6 SHINE")).isEqualTo("MANUAL");
        assertThat(extractTransmission("Citroen Berlingo 1.6 BlueHDI XTR 100 MAN")).isEqualTo("MANUAL");
        assertThat(looksLikelyFalseAutomatic("PEUGEOT 207 1.4 i BENZIN 70 kW NOVE ROZVODY", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Peugeot 308 1.6HDI 88KW 9/2015 LED NAVIGACE P. SERVIS", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Octavia III 2,0TDi 110KW Edition + NAVI tempomat ALU STK", "AUTOMATIC")).isTrue();
        assertThat(correctLikelyNoisyFuel("Toyota Sienna AWD 2017 7 mist 8AT tazne", "DIESEL")).isNull();
        assertThat(correctLikelyNoisyFuel("Dacia Sandero Stepway", "DIESEL")).isNull();
        assertThat(correctLikelyNoisyFuel("Navara D22 Kingcab 98kw", "PETROL")).isNull();
        assertThat(correctLikelyNoisyFuel("Opel Grandland X 1 majitel servis", "LPG")).isNull();
        assertThat(correctLikelyFalseElectricFuel("TOYOTA COROLLA 2022", "ELECTRIC")).isNull();
        assertThat(correctLikelyFalseElectricFuel("Toyota Mirai Executive", "ELECTRIC")).isEqualTo("ELECTRIC");
        assertThat(correctLikelyFalseElectricFuel("TOYOTA RAV4,Hybrid,Selection,4x4,Tazne", "ELECTRIC")).isEqualTo("HYBRID");

        assertThat(looksNonCarListing("Alfa Romeo 156 blatniky", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Znaky Alfa Romeo 74mm", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Hlinikove kryty pedalu Alfa 159", "", "", "")).isTrue();
        assertThat(looksNonCarListing("TI Zadni pruziny Alfa Romeo 159 1.9JTDm, JTS, 2.0, 2.2, 1.8", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Hlavice radicky Alfa Romeo 159", "", "", "")).isTrue();
        assertThat(looksNonCarListing("JTD Palivovy filtr", "", "", "")).isTrue();
        assertThat(looksNonCarListing(
                "Toyata Yaris 1.5 16V, Edice Y20, 41 tis km",
                "",
                "https://auto.bazos.cz/inzerat/219508952/toyata-yaris-15-16v-edice-y20-41-tis-km.php",
                "Toyata Yaris 1.5 16V, Edice Y20, 41 tis km")).isFalse();
        assertThat(looksNonCarListing("Zadni podbehy Alfa Giulia", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Tmave stinitka Alfa Romeo 147/GT", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Alfa Romeo 159 - tlacitka, ovladace", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Levy halogen Alfa Romeo Giulietta", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Original setrvacnik Lancia Fiat 1.2 8v / 16v", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Sada OEM filtru Alfa 159", "", "", "")).isTrue();
    }

    @Test
    void rejectsBareBrandTitlesButKeepsRealModels() throws Exception {
        assertThat(looksSuspiciousListing("Alfa romeo", "")).isTrue();
        assertThat(looksSuspiciousListing("Alfa Romeo 159", "")).isFalse();
        assertThat(looksSuspiciousListing("SKODA FABIA 3 1,0MPi 44kW Koup.CR,1.majitel,Serv. kniha,2019", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("SKODA SCALA 1,0TSi 70kW Koup.CR,1.majitel,LED,2022,119tkm", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("VW ARTEON 2,0TDi 110kW DSG ELEGANCE CR 2022 NYNI PO SERVISE", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("SKODA OCTAVIA IV 1,5TSi G-TEC 96kW Koup.CR,50.000km2022", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("OPEL e-CORSA 100kW ELEGANCE electro Koup.CR,1.majitel,2023", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("OPEL MOKKA 1,4T 112kW 4x4 Automat,TAZNE,Koup.CR,90.0000km", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("Peugeot 5008,automat, head up, 7mist", "rezervace")).isFalse();
    }

    @Test
    void keepsMazdaSixAsCarListing() throws Exception {
        assertThat(looksNonCarListing(
                "PRODAM MAZDU 6 GH VE VYBORNEM STAVU",
                "",
                "https://auto.bazos.cz/inzerat/219096536/prodam-mazdu-6-gh-ve-vybornem-stavu.php",
                ""))
                .isFalse();
    }

    @Test
    void rejectsKnownTitleUrlModelMismatch() throws Exception {
        assertThat(looksTitleUrlMismatch(
                "Doblo 1,4t-jet,88kw,dilna sortimo,1.maj.Cr,odpocet dph",
                "https://auto.bazos.cz/inzerat/218934074/kangoo-15dci-2020-149tiskm1majcrodpocet-dphl3.php"))
                .isTrue();
        assertThat(looksTitleUrlMismatch(
                "CITROEN C4 GRAND PICASSO 1.6i MANUAL LED KEYLESS TAZNE ZAR.",
                "https://auto.bazos.cz/inzerat/219556468/citroen-c4-picasso-16i-manual-led-keyless-go-tazne-zarizeni.php"))
                .isFalse();
    }

    @Test
    void keepsCommercialTransitCustomOutOfPassengerResults() throws Exception {
        assertThat(looksCommercialVehicle(
                "FORD Transit CUSTOM 2,2 tdci 114kw L1 H1 Navigace",
                "",
                "https://auto.bazos.cz/inzerat/219364003/ford-transit-custom-22-tdci-114kw-l1-h1.php"))
                .isTrue();

        assertThat(looksCommercialVehicle(
                "Tourneo Custom Titanium X L2 odpocet DPH nova prevodovka",
                "",
                "https://auto.bazos.cz/inzerat/219087044/tourneo-custom-titanium-x-l2-odpocet-dph.php"))
                .isFalse();

        assertThat(looksCommercialVehicle(
                "PEUGEOT EXPERT 1.6 hdi 2008",
                "",
                "https://auto.bazos.cz/inzerat/219457769/peugeot-expert-16-hdi-2008.php"))
                .isTrue();

        assertThat(looksCommercialVehicle(
                "Toyota Proace Verso XL, zakoupena nova v CR",
                "",
                "https://auto.bazos.cz/inzerat/219323068/jsjd.php"))
                .isFalse();

        assertThat(looksCommercialVehicle(
                "Mercedes-Benz C180 Kompressor W204 - 115 kW - manual",
                "",
                "https://auto.bazos.cz/inzerat/218778125/mercedes-benz-c180-kompressor-w204-115-kw-manual.php"))
                .isFalse();

        assertThat(looksCommercialVehicle(
                "Citroen HY - W",
                "",
                "https://auto.bazos.cz/inzerat/219491815/citroen-hy-w.php"))
                .isTrue();
    }

    @Test
    void buildsClassificationProfileFromBazosTitleOnly() throws Exception {
        Object profile = inferTitleProfile(
                "BMW e46 330ci, M Packet, 170KW, 231 hp",
                "https://auto.bazos.cz/inzerat/219160039/bmw-e46-330ci-m-packet-170kw-231-hp.php");

        assertThat(titleProfileValue(profile, "fuelType")).isEqualTo("PETROL");
        assertThat(titleProfileValue(profile, "carType")).isNull();
        assertThat(titleProfileFlag(profile, "strongIdentity")).isTrue();

        Object plugInProfile = inferTitleProfile(
                "VW Golf 8 GTE 1.4 TSI Hybrid 150kW DSG",
                "https://auto.bazos.cz/inzerat/219486608/vw-golf-8-gte-14-tsi-hybrid.php");

        assertThat(titleProfileValue(plugInProfile, "fuelType")).isEqualTo("PLUGIN_HYBRID");
        assertThat(titleProfileValue(plugInProfile, "transmission")).isEqualTo("AUTOMATIC");
        assertThat(titleProfileValue(plugInProfile, "carType")).isEqualTo("HATCHBACK");
        assertThat(titleProfileFlag(plugInProfile, "strongIdentity")).isTrue();
    }

    @Test
    void resolvesApproximateMileageFromBazosTitles() throws Exception {
        assertThat(extractMileage("Citroen C5 combi diesel 124 xxx km", "")).isEqualTo(124000);
        assertThat(extractMileage("Honda CR-V 2.0 e:HEV Advance AWD, r. 2024, najeto cca 15100", "")).isEqualTo(15100);
    }

    @Test
    void prefersYearFromTitleOverNoisyPageText() throws Exception {
        assertThat(extractYear("105.000km MITSUBISHI OUTLANDER III FL 2.0 MIVEC 2016", "r.v.2007"))
                .isEqualTo(2016);
        assertThat(extractYear("Nissan Qashqai 2011 Diesel 81Kw 201tis Km, Po servise a STK", ""))
                .isEqualTo(2011);
        assertThat(extractYear("Nissan Qashqai 2016r 112tis Najezd", "2012"))
                .isEqualTo(2016);
    }

    private String extractFuelType(String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("extractFuelType", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, text);
    }

    private String extractBrand(String title, String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("extractBrand", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text);
    }

    private String extractCarType(String title, String text, String url) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("extractCarType", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text, url);
    }

    private String extractTransmission(String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("extractTransmission", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, text);
    }

    private boolean looksLikelyFalseAutomatic(String title, String transmission) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksLikelyFalseAutomatic", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, transmission);
    }

    private String correctLikelyNoisyFuel(String title, String fuelType) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("correctLikelyNoisyFuel", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, fuelType);
    }

    private String correctLikelyFalseElectricFuel(String title, String fuelType) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("correctLikelyFalseElectricFuel", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, fuelType);
    }

    private Integer extractMileage(String title, String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("extractMileage", String.class, String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, title, text);
    }

    private Integer extractYear(String title, String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("extractYear", String.class, String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, title, text);
    }

    private boolean looksSuspiciousListing(String title, String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksSuspiciousListing", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, text);
    }

    private boolean looksNonCarListing(String title, String text, String url, String analysisText) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksNonCarListing", String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, text, url, analysisText);
    }

    private boolean looksTitleUrlMismatch(String title, String url) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksTitleUrlMismatch", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, url);
    }

    private boolean looksCommercialVehicle(String title, String text, String url) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksCommercialVehicle", String.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, text, url);
    }

    private Object inferTitleProfile(String title, String url) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("inferTitleProfile", String.class, String.class);
        method.setAccessible(true);
        return method.invoke(parser, title, url);
    }

    private String titleProfileValue(Object profile, String methodName) throws Exception {
        Method method = profile.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(profile);
    }

    private boolean titleProfileFlag(Object profile, String methodName) throws Exception {
        Method method = profile.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (boolean) method.invoke(profile);
    }
}
