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
        assertThat(extractBrand("♦️SEAT ALHAMBRA 2.0TDI 103KW 7MIST TAZNE Z.", "volkswagen sharan"))
                .isEqualTo("SEAT");
    }

    @Test
    void resolvesFuelFromSuzukiBazosTitles() throws Exception {
        assertThat(extractFuelType("Suzuki Vitara 1.6 DDiS AllGrip 4x4")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Suzuki Grand Vitara 2.4 VVT")).isEqualTo("PETROL");
        assertThat(extractFuelType("Suzuki Vitara 1.4 BoosterJet AllGrip Mild-Hybrid r2020")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Suzuki Virara 1.6 Ddis")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Suzuki Alto,1.0i,50kw")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodám Suzuki s×4,1.6")).isEqualTo("PETROL");
    }

    @Test
    void resolvesCarTypesFromBazosTitles() throws Exception {
        assertThat(extractCarType("Seat Leon1.5 TSi 96kW 1majitel CR Xcellence", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Seat Leon ST 1.2 TSI, 81kW, r2017", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Seat Altea XL 1.6 TDI 77 kW Automat", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Seat ibiza", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Suzuki Jimny 1.3 i 2015", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("suzuki jimny 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Prodám Suzuki s×4,1.6", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Virara 1.6 Ddis", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Alto,1.0i,50kw", "", "")).isEqualTo("HATCHBACK");
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
}
