package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SbazarParserTest {

    private final SbazarParser parser = new SbazarParser();

    @Test
    void resolvesFuelFromTrustedListingIdentity() throws Exception {
        assertThat(resolveFuelType("vw id 3 pro performance rv.2024", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("vw id 3 pro performance rv.2024", "", "ELECTRIC")).isEqualTo("AUTOMATIC");

        assertThat(resolveFuelType("smart forfour eq comfort 60 kw", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("smart forfour eq comfort 60 kw", "", "ELECTRIC")).isEqualTo("AUTOMATIC");

        assertThat(resolveFuelType("volvo xc60 t6 awd recharge led panorama", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("volvo xc60 t6 224kw 4x4 r-design navi", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("volvo xc40 2,0 d3 inscription", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("bmw rada 2 218i active tourer at led", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("alfa romeo spider 2.2i jts 185ps exclusive", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("bmw rada 3 330e xdrive m-paket propano", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("ford escort 1.6 66kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("volkswagen multivan 2.0tdi 103kw highline 4motion", "")).isEqualTo("DIESEL");
        assertThat(resolveTransmission("volkswagen multivan 2.0tdi 103kw highline 4motion", "", "DIESEL")).isEqualTo("-");
        assertThat(resolveFuelType("hyundai ioniq 5 style 77,4 kwh soh 96,3 800v", "")).isEqualTo("ELECTRIC");
        assertThat(resolveFuelType("mercedes-benz tridy c 43 amg 4matic", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes r129 sl320 zehlicka", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("nova stk klima alu tazne ford fiesta 1.4 59 kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("klima bez koroze 184 tis volkswagen golf 1.6", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("volkswagen golf, 1.4-59 kw,klima,r.09,nova stk", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes benz a 45 amg", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("jaecoo 7 1.6 phev shs exclusive", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("omoda 9 shs premium", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("hyundai inster cross premium", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("omoda 5 1.6 t 108 kw premium a/t", "", "PETROL")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("mazda cx-5 2.2 skyactive 4wd bose sendo", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("mazda cx-3 2.0 sky-g 121k attraction a/t", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("alfa romeo 147 gta 3,2busso manual", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("chevrolet matiz 0.8 rok 2009 krasny stav", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford s-max 1.5 st-line 118 kw 1 majitel", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford b-max 1.0 74kw colourline", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("citroen c4 1.2 e-thp 81kw shine", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("skoda fabia 1,2 htp classic", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("dacia jogger expression eco-g 120 5 mist", "")).isEqualTo("LPG");
        assertThat(resolveFuelType("tesla model 3 performance", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("honda crv 2020 hybrid benzin 72tis.km", "", "HYBRID")).isEqualTo("AUTOMATIC");
        assertThat(resolveTransmission("tesla model 3 long range dual motor soh 92", "", "ELECTRIC")).isEqualTo("AUTOMATIC");
    }

    @Test
    void ignoresNoisyScopedFuelWhenTitleHasNoMatchingSignal() throws Exception {
        String identity = "passat b7 senzory zadni";
        String noisyScopedText = identity + " cng lpg diesel benzin";

        assertThat(resolveFuelType(identity, noisyScopedText)).isEqualTo("-");
    }

    @Test
    void detectsBrandsPresentInSbazarLogs() throws Exception {
        assertThat(detectBrand("bentley continental gt v12 434 kw breitling masaze")).isEqualTo("BENTLEY");
        assertThat(detectBrand("hyundai ioniq 5 style 77,4 kwh")).isEqualTo("HYUNDAI");
        assertThat(detectBrand("smart forfour eq comfort 60 kw")).isEqualTo("SMART");
        assertThat(detectBrand("jaecoo 7 jaecoo 4x4 exclusive")).isEqualTo("JAECOO");
        assertThat(detectBrand("tesla model 3 performance")).isEqualTo("TESLA");
        assertThat(detectBrand("ssangyong tivoli xlv 1.6i 94kw lpg")).isEqualTo("SSANGYONG");
    }

    @Test
    void resolvesCarTypesFromSbazarLogTitles() throws Exception {
        assertThat(resolveCarType("renault kadjar 1,3tce edc d klima navi kamera", "")).isEqualTo("SUV");
        assertThat(resolveCarType("skoda fabie 1,4 mpi 44kw", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("bmw 116i f20 urban line 100kw rok 2013 automat", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("ford escort 1.6 66kw", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("volkswagen cc facelift passat 2.0tdi 103kw m2013", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes c220 w204 2.2cdi avantgarde", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes-benz tridy e 220cdi amg paket", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("audi a8 3,0 50 tdi quattro laser dph", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes-benz tridy c 43 amg 4matic", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("vw id 3 pro performance rv.2024", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("smart fourtwo 451 cdi l6e", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("smart forfour eq comfort 60 kw", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("hyundai ix20 1.4 crdi 60tkm cz", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("mazda cx-7 2.3 lpg 4x4 automat", "")).isEqualTo("SUV");
        assertThat(resolveCarType("mazda cx7 2.2 mzr 127 kw", "")).isEqualTo("SUV");
        assertThat(resolveCarType("hyundai ioniq 5 style 77,4 kwh soh 96,3 800v", "")).isEqualTo("SUV");
        assertThat(resolveCarType("opel meriva 1.7dci 74 kw", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("peugeot partner tepee 1.6 vti 72kw", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("bmw rada 4 m440i cr hk tazne 2x kola", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("bmw rada 5 530d xdrive luxury line daprof nafta", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("vw arteon shooting brake 147kw r-line dsg 06/2021", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("nissan padfinder", "")).isEqualTo("SUV");
        assertThat(resolveCarType("audi a4 avant 1,4 tfsi aut led navi e kufr", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("mercedes benz a 45 amg", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("hyundai bayon bayon fl 1,0 t-gdi wave", "")).isEqualTo("SUV");
        assertThat(resolveCarType("hyundai inster cross premium", "")).isEqualTo("SUV");
        assertThat(resolveCarType("jaecoo 7 1.6 phev shs exclusive", "")).isEqualTo("SUV");
        assertThat(resolveCarType("omoda 9 shs premium", "")).isEqualTo("SUV");
        assertThat(resolveCarType("peugeot 3008 allure hybrid extra-stav nove", "")).isEqualTo("SUV");
        assertThat(resolveCarType("suzuki grand vitara 2,4i", "")).isEqualTo("SUV");
        assertThat(resolveCarType("hyundai i30 cw 1.6d r.v. 2010", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("prodam dacia logan mcv 1.2 55 kw 2014", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("kia proceed gt 1.6 t-gdi 150 kw r.v. 2023", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("mercedes-benz cls 1 majitel amg full led 4 matic", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("hyundai bayon bayon fl 1,0 t-gdi wave", "combi wagon")).isEqualTo("SUV");
        assertThat(resolveCarType("skoda roomster 1.2tsi 63kw nove v cr klima", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("audi a6 3.0 tdi 4x4 avant quattro automat sline", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("fiat bravo 2011 1.4 66kw lpg servis nove rozvody", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("chevrolet matiz 0.8 rok 2009 krasny stav", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("mercedes w 220 320 i", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("tesla model 3 performance", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("ford b-max 1.0 74kw colourline", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("volkswagen t5 2.0tdi 9mist", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("dacia jogger expression eco-g 120 5 mist", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("ssangyong tivoli xlv 1.6i 94kw lpg", "")).isEqualTo("SUV");
        assertThat(resolveCarType("renault fluence 1.5 dci 81 kw klimatizace", "")).isEqualTo("SEDAN");
    }

    @Test
    void skipsNonCarSbazarListingsFromFreshLogs() throws Exception {
        assertThat(looksNonCarListing("padlo a rucni pumpicka")).isTrue();
    }

    private String resolveFuelType(String identityText, String scopedText) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("resolveFuelType", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, identityText, scopedText);
    }

    private String resolveTransmission(String identityText, String scopedText, String fuelType) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("resolveTransmission", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, identityText, scopedText, fuelType);
    }

    private String detectBrand(String searchable) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("detectBrand", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, searchable);
    }

    private String resolveCarType(String identityText, String scopedText) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("resolveCarType", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, identityText, scopedText);
    }

    private boolean looksNonCarListing(String searchable) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("looksNonCarListing", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, searchable);
    }
}
