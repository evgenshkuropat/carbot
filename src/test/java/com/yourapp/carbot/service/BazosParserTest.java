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
    }

    @Test
    void resolvesFuelFromMercedesAndMitsubishiBazosTitles() throws Exception {
        assertThat(extractFuelType("Mercedes-Benz E 350 BLUETEC 4MATIC 12/2016")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Mercedes-Benz 126.500 SEC AMG Paket")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodam Mitsubishi Eclipse cross 1,5")).isEqualTo("PETROL");
        assertThat(extractFuelType("Alfa Romeo 156 2.0 JTS 16V Selespeed Distinctive Funny car")).isEqualTo("PETROL");
        assertThat(extractFuelType("Alfa Romeo Stelvio 2.0 Turbo 16V AT8-Q4 Veloce T")).isEqualTo("PETROL");
        assertThat(extractFuelType("Honda HR-V 1.5 benzin hybrid")).isEqualTo("HYBRID");
        assertThat(extractFuelType("FIAT 500 1.0 11/2022 DPH 61000km zanovni")).isEqualTo("PETROL");
        assertThat(extractFuelType("Fiat 500 / 0.9 TwinAir / SPORT / 77kW / NAVI")).isEqualTo("PETROL");
    }

    @Test
    void resolvesCarTypesFromBazosTitles() throws Exception {
        assertThat(extractCarType("Seat Leon1.5 TSi 96kW 1majitel CR Xcellence", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Seat Leon ST 1.2 TSI, 81kW, r2017", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Seat Altea XL 1.6 TDI 77 kW Automat", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Seat ibiza", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Suzuki Jimny 1.3 i 2015", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("suzuki jimny 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Prodam Suzuki sx4,1.6", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Virara 1.6 Ddis", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Alto,1.0i,50kw", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Audi S3", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Mini Cooper 1.5 i 2018 F 56", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("BMW 325i e91", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("BMW M3 MANUAL KOMPRESOR", "", "")).isEqualTo("COUPE");
    }

    @Test
    void resolvesMercedesAndMazdaBodyTypesFromBazosTitles() throws Exception {
        assertThat(extractCarType("MERCEDES BENZ CLA 200 SHOOTING BRAKE AMG", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("E220 All-Terrain 4Matic", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes Benz C 220 CDI T BlueEfficiency (W204)", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes e270cdi rv2000", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes C200CDI, r.v. 2003, 85kW, automat", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes-Benz C 250d 150kW AMG 4MATIC KEYLESS", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mercedes-Benz 126.500 SEC AMG Paket", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("PRODAM MAZDU 6 GH VE VYBORNEM STAVU", "", "")).isEqualTo("SEDAN");
    }

    @Test
    void resolvesAlfaRomeoTransmissionAndSkipsPartTitles() throws Exception {
        assertThat(extractTransmission("Alfa Romeo 156 2.0 JTS 16V Selespeed Distinctive Funny car")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Alfa Romeo Stelvio 2.0 Turbo 16V AT8-Q4 Veloce T")).isEqualTo("AUTOMATIC");

        assertThat(looksNonCarListing("Alfa Romeo 156 blatniky", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Znaky Alfa Romeo 74mm", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Hlinikove kryty pedalu Alfa 159", "", "", "")).isTrue();
        assertThat(looksNonCarListing("TI Zadni pruziny Alfa Romeo 159 1.9JTDm, JTS, 2.0, 2.2, 1.8", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Hlavice radicky Alfa Romeo 159", "", "", "")).isTrue();
        assertThat(looksNonCarListing("JTD Palivovy filtr", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Zadni podbehy Alfa Giulia", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Tmave stinitka Alfa Romeo 147/GT", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Alfa Romeo 159 - tlacitka, ovladace", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Original setrvacnik Lancia Fiat 1.2 8v / 16v", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Sada OEM filtru Alfa 159", "", "", "")).isTrue();
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

    private boolean looksNonCarListing(String title, String text, String url, String analysisText) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksNonCarListing", String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, text, url, analysisText);
    }
}
