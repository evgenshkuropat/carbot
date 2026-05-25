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
        assertThat(normalizeFuelType(null, "Mazda CX-5 2.2 SkyActive 4WD, BOSE")).isEqualTo("DIESEL");
        assertThat(normalizeFuelType(null, "Mazda CX-3, 2.0 Sky-G 121k Attraction A/T")).isEqualTo("PETROL");
        assertThat(normalizeFuelType("DIESEL", "Chevrolet Matiz 0.8, rok 2009")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Alfa Romeo 147 GTA 3,2Busso manual")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Tesla Model 3 Performance")).isEqualTo("ELECTRIC");
        assertThat(normalizeFuelType(null, "Dacia Jogger Expression Eco-G 120")).isEqualTo("LPG");
        assertThat(normalizeFuelType(null, "Ford B-MAX 1.0 74kW ColourLine")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Citroen C4 1.2 e-THP 81kW")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Skoda Fabia 1.2 HTP Classic")).isEqualTo("PETROL");
    }

    @Test
    void normalizesCommonModelBodyTypesBeforeKeepingParserValue() throws Exception {
        assertThat(normalizeCarType("WAGON", "Volkswagen Multivan 2,0 TDI 84kw Startline")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("MINIVAN", "VW TIGUAN 2.0TDI4x4,Automat,NAVI,TOP STAV")).isEqualTo("SUV");
        assertThat(normalizeCarType("WAGON", "Skoda Yeti 1,4 Tsi 90kw Monte Carlo")).isEqualTo("SUV");
        assertThat(normalizeCarType(null, "Skoda Roomster, 1.2TSi 63kW")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("HATCHBACK", "Citroen C4 Picasso, 1.6i LPG Tazne")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("SEDAN", "Audi A6 3.0 TDI 4X4,Avant,quattro")).isEqualTo("WAGON");
        assertThat(normalizeCarType(null, "Fiat Bravo 2011 1.4 66kW LPG")).isEqualTo("HATCHBACK");
        assertThat(normalizeCarType(null, "Chevrolet Matiz 0.8, rok 2009")).isEqualTo("HATCHBACK");
        assertThat(normalizeCarType(null, "Tesla Model 3 Performance")).isEqualTo("SEDAN");
        assertThat(normalizeCarType("HATCHBACK", "Volkswagen T5 2.0Tdi 9mist")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType(null, "Dacia Jogger Expression Eco-G 120")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("HATCHBACK", "SsangYong Tivoli XLV 1.6i LPG")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Renault Fluence 1.5 dCi")).isEqualTo("SEDAN");
    }

    @Test
    void normalizesHondaHybridTransmission() throws Exception {
        assertThat(normalizeTransmission(null, "HONDA CRV 2020 hybrid benzin", "HYBRID")).isEqualTo("AUTOMATIC");
        assertThat(normalizeTransmission(null, "Tesla Model 3 Performance", "ELECTRIC")).isEqualTo("AUTOMATIC");
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

    private String normalizeTransmission(String transmission, String title, String fuelType) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("normalizeTransmission", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, transmission, title, fuelType);
    }

    private boolean looksLikeBadTitle(String title) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("looksLikeBadTitle", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, title);
    }
}
