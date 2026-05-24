package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CarStorageServiceTest {

    private final CarStorageService service = new CarStorageService(null);

    @Test
    void normalizesElectricSignalsFromTitles() throws Exception {
        assertThat(normalizeFuelType(null, "Tesla Model Y")).isEqualTo("ELECTRIC");
        assertThat(normalizeFuelType(null, "Polestar 2 EV 350kW AWD 78kWh Performance")).isEqualTo("ELECTRIC");
        assertThat(normalizeFuelType(null, "Nissan LEAF ELEKTRIC VISIA 1 MAJITEL")).isEqualTo("ELECTRIC");
        assertThat(normalizeFuelType(null, "Mini Ostatni Cooper SE elektricky")).isEqualTo("ELECTRIC");
    }

    @Test
    void normalizesCommonModelBodyTypesBeforeKeepingParserValue() throws Exception {
        assertThat(normalizeCarType("WAGON", "Volkswagen Multivan 2,0 TDI 84kw Startline")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("MINIVAN", "VW TIGUAN 2.0TDI4x4,Automat,NAVI,TOP STAV")).isEqualTo("SUV");
        assertThat(normalizeCarType("WAGON", "Skoda Yeti 1,4 Tsi 90kw Monte Carlo")).isEqualTo("SUV");
    }

    @Test
    void rejectsGenericCarTitles() throws Exception {
        assertThat(looksLikeBadTitle("Osobni automobil")).isTrue();
    }

    private String normalizeFuelType(String fuelType, String title) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("normalizeFuelType", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, fuelType, title);
    }

    private String normalizeCarType(String carType, String title) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("normalizeCarType", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, carType, title);
    }

    private boolean looksLikeBadTitle(String title) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("looksLikeBadTitle", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, title);
    }
}
