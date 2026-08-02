package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
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
        assertThat(normalizeFuelType(null, "Opel Zafira A 2,0 74 kW")).isEqualTo("DIESEL");
        assertThat(normalizeFuelType(null, "Peugeot 3008 1.2 96kW")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Fiat 500X 1.6 81kw")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Mercedes-Benz CL 500")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Jaguar XK8 4.0 209 kW")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Nissan Qashqai TEKNA e-POWER 2WD 140kW")).isEqualTo("HYBRID");
        assertThat(normalizeFuelType(null, "BMW Rada 2 225xe iPERFORMANCE F45 165kW")).isEqualTo("HYBRID");
        assertThat(normalizeFuelType(null, "TOP MOTOR 4x4 1.8 T 110 KW SKODA OCTAVIA Super")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Chevrolet Cruze 1.6 91kW")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Ford Focus Combi 1.6 85 kW")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Ford Fiesta ST atmosfera 150PS")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Suzuki SX4 S-Cross 1,4 BoosterJet Premium")).isEqualTo("PETROL");
        assertThat(normalizeFuelType(null, "Dongfeng U-Tour 1,5 T 130 kW ExclusiveFR")).isEqualTo("PETROL");
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
        assertThat(normalizeCarType("HATCHBACK", "Volkswagen T6 2.0 TDI 84 kW")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType(null, "Dacia Jogger Expression Eco-G 120")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("HATCHBACK", "SsangYong Tivoli XLV 1.6i LPG")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Renault Fluence 1.5 dCi")).isEqualTo("SEDAN");
        assertThat(normalizeCarType("COUPE", "Audi S8 TFSi QUATTRO")).isEqualTo("SEDAN");
        assertThat(normalizeCarType("HATCHBACK", "Jaguar XF 2.0 25D")).isEqualTo("SEDAN");
        assertThat(normalizeCarType("CABRIO", "Renault Koleos 4X4 2.0DCi")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Fiat 500X 1.6 81kw")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Land Rover Freelander 2.0TD4")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Dacia Bigster Journey hybrid 155")).isEqualTo("SUV");
        assertThat(normalizeCarType(null, "Mercedes-Benz CL 500")).isEqualTo("COUPE");
        assertThat(normalizeCarType("HATCHBACK", "Jaguar XK8 4.0 209 kW")).isEqualTo("COUPE");
        assertThat(normalizeCarType(null, "Dodge Caliber")).isEqualTo("HATCHBACK");
        assertThat(normalizeCarType("HATCHBACK", "Ford Tourneo Custom 2,0 EcoBlue 96kW 8mist")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType(null, "Ford Tourneo Courier Active, Tourneo, 1.0 EcoBoost")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("HATCHBACK", "Ford Puma Titanium, 5dverova, 1.0 EcoBoost")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Subaru Outback 2.5 lpg r.v. 2006")).isEqualTo("WAGON");
        assertThat(normalizeCarType("HATCHBACK", "Alpina XD3 3.0d")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Renault Captur, techno mildhybrid 140 EDC MY25")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Skoda Elroq, 55 125 kW Selection")).isEqualTo("SUV");
        assertThat(normalizeCarType("WAGON", "Volkswagen Caddy 2.0TDI 75kW TAZNE CZ DPH")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("PICKUP", "SUZUKI JIMNY 1.3VVTi - 2016 - 64.000 km")).isEqualTo("SUV");
        assertThat(normalizeCarType(null, "Toyota PROACE VERSO 2,0 D-4D L2 Family AT")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("VAN", "Renault Trafic")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("VAN", "Opel Vivaro 2,5 L2H1 WESTFALIA LIFE POSTEL")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("VAN", "Nissan Primastar 2.0 dCi")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("PICKUP", "RAM 1500 5.7 V8 401k LIMITED NIGHT RAMB")).isEqualTo("PICKUP");
        assertThat(normalizeCarType("HATCHBACK", "Dongfeng U-Tour 1,5 T 130 kW ExclusiveFR 7mist")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("HATCHBACK", "Dongfeng Mage 1,5 T 145 kW E2 DCT7")).isEqualTo("SUV");
        assertThat(normalizeCarType("MINIVAN", "Dongfeng T5 EVO 1,5 T 130kW DCT7 DragonEdition")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "BMW X2 sDrive 18d M-PAKET")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Seat Arona 1,6 TDI 85kW FR")).isEqualTo("SUV");
        assertThat(normalizeCarType(null, "Alfa Romeo Stelvio 2.2 JTDm Competizione Q4")).isEqualTo("SUV");
        assertThat(normalizeCarType("HATCHBACK", "Suzuki SX4 S-Cross 1,4 BoosterJet Premium")).isEqualTo("SUV");
        assertThat(normalizeCarType(null, "Skoda Octavia Scout CZ DPH")).isEqualTo("WAGON");
        assertThat(normalizeCarType("HATCHBACK", "BMW Rada 2 225xe iPERFORMANCE F45 165kW")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("WAGON", "Citroen C8 2.2HDI/AT/CR/Exclusive/7.Mist")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("WAGON", "Citroen Berlingo 1,6HDi 2xSOUPACKY")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("WAGON", "Peugeot Partner 1,6HDi 2xSOUPACKY")).isEqualTo("MINIVAN");
        assertThat(normalizeCarType("WAGON", "Renault Twingo 1.2 Dynamique PANORAMA")).isEqualTo("HATCHBACK");
    }

    @Test
    void normalizesHondaHybridTransmission() throws Exception {
        assertThat(normalizeTransmission(null, "HONDA CRV 2020 hybrid benzin", "HYBRID")).isEqualTo("AUTOMATIC");
        assertThat(normalizeTransmission(null, "Tesla Model 3 Performance", "ELECTRIC")).isEqualTo("AUTOMATIC");
    }

    @Test
    void normalizesAdditionalBrandsFromFreshLogs() throws Exception {
        assertThat(normalizeBrand("ISUZU", "Isuzu D-Max V-Cross 2.2L")).isEqualTo("ISUZU");
        assertThat(normalizeBrand(null, "Cadillac ATS V")).isEqualTo("CADILLAC");
        assertThat(normalizeBrand(null, "Alpina, XD3 3.0d")).isEqualTo("ALPINA");
        assertThat(normalizeBrand(null, "Lancia Kappa 2.4JTD 10V Klima")).isEqualTo("LANCIA");
    }

    @Test
    void repairsMojibakeBeforeStorageNormalization() throws Exception {
        assertThat(cleanText("BMW X3 M50 xDrive, Nez\u0102\u02C7visl\u0102\u00A9, Ta\u0139\u013En\u0102\u00A9"))
                .isEqualTo("BMW X3 M50 xDrive, Nez\u00E1visl\u00E9, Ta\u017En\u00E9");
        assertThat(cleanText("Toyota ProAce Verso 2.0D, \u00C4\u015AR-1m, Family L2, DPH"))
                .isEqualTo("Toyota ProAce Verso 2.0D, \u010CR-1m, Family L2, DPH");
        assertThat(cleanText("\u0139\u00A0koda Scala 1.0TGI STYLE ACC 2XKOLA LED"))
                .isEqualTo("\u0160koda Scala 1.0TGI STYLE ACC 2XKOLA LED");
        assertThat(cleanText("Citro\u0102\u00ABn C3 1,4 HDi CZ 1 maj DPH"))
                .isEqualTo("Citro\u00EBn C3 1,4 HDi CZ 1 maj DPH");
        assertThat(cleanText("Mlad\u0102\u02C7 Boleslav")).isEqualTo("Mlad\u00E1 Boleslav");
    }

    @Test
    void keepsRealCitroenWithExtraWheelsAndRejectsAirConditioningService() throws Exception {
        CarDto citroen = car("Citroen C3 1,2 i 60kW klima,zaves,2x kola");
        citroen.setBrand("CITROEN");
        citroen.setFuelType("PETROL");
        citroen.setCarType("HATCHBACK");
        assertThat(isValidForSave(citroen, 129_000)).isTrue();

        CarDto serviceListing = car("Prodej + montaz + servis klimatizaci dle vykonu");
        serviceListing.setCarType("SEDAN");
        assertThat(isValidForSave(serviceListing, 20_000)).isFalse();
    }

    @Test
    void keepsSaabWithExtraWheelsAndRamPickup() throws Exception {
        CarDto saab = car("Saab 9-3 2.0TiD 110KW man - 2x KOLA");
        saab.setBrand("SAAB");
        saab.setFuelType("DIESEL");
        saab.setCarType("WAGON");
        assertThat(isValidForSave(saab, 129_000)).isTrue();

        CarDto ram = car("RAM 1500 TRX 6.2 V8");
        ram.setBrand("RAM");
        ram.setFuelType("PETROL");
        ram.setCarType("PICKUP");
        assertThat(isValidForSave(ram, 1_500_000)).isTrue();
    }

    @Test
    void keepsPassengerVansAndRejectsPlainCommercialVans() throws Exception {
        CarDto passengerVivaro = car("Opel Vivaro 2.5 CDTI Tour Cosmo");
        passengerVivaro.setBrand("OPEL");
        passengerVivaro.setFuelType("DIESEL");
        passengerVivaro.setCarType("MINIVAN");
        assertThat(isValidForSave(passengerVivaro, 279_000)).isTrue();

        CarDto cargoVivaro = car("Opel Vivaro 2.0 CDTI L2H1");
        cargoVivaro.setBrand("OPEL");
        cargoVivaro.setFuelType("DIESEL");
        cargoVivaro.setCarType("MINIVAN");
        assertThat(isValidForSave(cargoVivaro, 279_000)).isFalse();
    }

    @Test
    void rejectsGenericCarTitles() throws Exception {
        assertThat(looksLikeBadTitle("Osobni automobil")).isTrue();
        assertThat(looksLikeBadTitle("Skoda")).isTrue();
        assertThat(looksLikeBadTitle("BMW")).isTrue();
        assertThat(looksLikeBadTitle("Toyota")).isTrue();
        assertThat(looksLikeBadTitle("Toyota Camry, 2.5 Hybrid Executive REZERVACE")).isTrue();
        assertThat(looksLikeBadTitle("Seat Tarraco, 2,0 TDI 4x4 DSG PŘIPRAVUJEME")).isTrue();
        assertThat(looksLikeBadTitle("BMW X5")).isFalse();
        assertThat(looksLikeBadTitle("Volkswagen Golf")).isFalse();
        assertThat(looksLikeBadTitle("Volkswagen T-Roc, 2.0 TDI 110 kW DSG SPORT")).isFalse();
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

    private String normalizeBrand(String brand, String title) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("normalizeBrand", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, brand, title);
    }

    private String cleanText(String value) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("cleanText", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, value);
    }

    private CarDto car(String title) {
        return new CarDto("TEST", title, "129000", 129_000, "Praha",
                "https://example.com/car", null);
    }

    private boolean isValidForSave(CarDto car, Integer priceValue) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("isValidForSave", CarDto.class, Integer.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, car, priceValue);
    }

    private boolean looksLikeBadTitle(String title) throws Exception {
        Method method = CarStorageService.class.getDeclaredMethod("looksLikeBadTitle", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, title);
    }
}
