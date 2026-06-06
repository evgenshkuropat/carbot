package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ToyotaProvereneVozyParserTest {

    private final ToyotaProvereneVozyParser parser = new ToyotaProvereneVozyParser();

    @Test
    void resolvesBodyTypesFromToyotaProvereneTitles() throws Exception {
        assertThat(extractCarType("Ford EcoSport 1,0EcoBoost 92kW M/T", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Toyota Auris 1.8 Hybrid AT SELECTION TS", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Lexus LC LC 500 Sport+", ""))
                .isEqualTo("COUPE");
        assertThat(extractCarType("Kia Stinger 2.2 CRDi 147kW 8AT GT-Line", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Skoda Octavia 1.2 TSI / 77 kW", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Aygo X 1.5 Hybrid 116k", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Toyota Avensis", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Corolla 1,8 HEV Executive záruka 3+2 roky", ""))
                .isEqualTo("HATCHBACK");
        assertThat(extractCarType("Toyota Corolla 1,8 HEV Comfort Tech TS", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Peugeot 3008 ALLURE 1.2 PureTech", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Ford B-MAX EcoBoost", ""))
                .isEqualTo("MINIVAN");
    }

    @Test
    void keepsPlugInHybridWhenModelAndPlugInAreJoined() throws Exception {
        assertThat(mapElectrifiedFuel("Toyota RAV4 2.5Plug-in Hybrid 4x4 304k"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(mapElectrifiedFuel("Toyota RAV4 2.5Plug-in 4x4 304k"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(mapElectrifiedFuel("Toyota C-HR 2,0 PHEV E-CVT Style"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(mapElectrifiedFuel("Volvo XC 40 B3 Plus Bright DCT"))
                .isEqualTo("HYBRID");
    }

    @Test
    void repairsToyotaProvereneMojibakeBeforeOutput() throws Exception {
        assertThat(repairMojibake("Ĺ koda Fabia 1.2 HTP / 51 kW"))
                .isEqualTo("Škoda Fabia 1.2 HTP / 51 kW");
        assertThat(repairMojibake("TSUSHO ModĹ™any - SkladovĂ© vozy"))
                .isEqualTo("TSUSHO Modřany - Skladové vozy");
        assertThat(repairMojibake("Toyota Yaris 1.5 Hybrid - K odbÄ›ru IHNED"))
                .isEqualTo("Toyota Yaris 1.5 Hybrid - K odběru IHNED");
        assertThat(repairMojibake("CitroĂ«n C4 1.2PureTech"))
                .isEqualTo("Citroën C4 1.2PureTech");
        assertThat(repairMojibake("Mazda CX-7 AWD, ZĂVÄšS"))
                .isEqualTo("Mazda CX-7 AWD, ZÁVĚS");
    }

    private String extractCarType(String title, String text) throws Exception {
        Method method = ToyotaProvereneVozyParser.class.getDeclaredMethod("extractCarType", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text);
    }

    private String mapElectrifiedFuel(String value) throws Exception {
        Method method = ToyotaProvereneVozyParser.class.getDeclaredMethod("mapElectrifiedFuel", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
    }

    private String repairMojibake(String value) throws Exception {
        Method method = ToyotaProvereneVozyParser.class.getDeclaredMethod("repairMojibake", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
    }
}
