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
        assertThat(resolveFuelType("volvo xc40 2,0 d3 inscription", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("bmw rada 2 218i active tourer at led", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("alfa romeo spider 2.2i jts 185ps exclusive", "")).isEqualTo("PETROL");
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
}
