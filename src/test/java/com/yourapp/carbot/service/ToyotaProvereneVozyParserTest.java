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
        assertThat(extractCarType("Lexus RC 5,0 Track Edition", ""))
                .isEqualTo("COUPE");
        assertThat(extractCarType("Kia Stinger 2.2 CRDi 147kW 8AT GT-Line", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Skoda Octavia 1.2 TSI / 77 kW", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Skoda Octavia 2.0TDI 103kW Elegance Edition Combi", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Skoda Fabia 1.2HTP ambiente combi", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Toyota Aygo X 1.5 Hybrid 116k", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Toyota Avensis", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Corolla 1,8 HEV Executive záruka 3+2 roky", ""))
                .isEqualTo("HATCHBACK");
        assertThat(extractCarType("Toyota Corolla 1,8 HEV Comfort Tech TS", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Toyota Corolla 2,0 HEV Executive TS", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Peugeot 3008 ALLURE 1.2 PureTech", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Ignis 1,2 DualJet Premium", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Ford B-MAX EcoBoost", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Kia Venga 1.6i, CR-1m, 2x kola, manual", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Citroen Jumper 2.2HDi, L1H1, CR-1, tazne, DPH", ""))
                .isEqualTo("VAN");
        assertThat(extractCarType("Citroen Jumpy Combi 2,0", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Ford Transit Courier 1,0 ECB Trend", ""))
                .isEqualTo("VAN");
        assertThat(extractCarType("Lexus LBX 1,5 HEV Original Edition 4x4", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Baic BJ30 BJ30 HEV 1.5T 209kW 2DHT 4x4 ALL IN MY25", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("MG Cyberster GT EV 77kWh 375kW 4x4", ""))
                .isEqualTo("CABRIO");
        assertThat(extractCarType("Subaru OUTBACK 2.5i,129kW,4x4,CZ,1Maj,Tazne", ""))
                .isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes-Benz GLE 450 4MATIC AMG Styling", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Skoda Yeti 1.8TSI 112kW Elegance Outdoor 4x4 DSG", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Ford EDGE Vignale 2.0 TDCi AWD", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Jeep Wrangler SAHARA 2.0T 4x4 Unlimited Sport Auto", ""))
                .isEqualTo("SUV");
        assertThat(extractCarType("Hyundai ix20 1.4 CVVT Trikolor, 1.maj., CZ", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Peugeot Partner 1,6i 72kW M/T 5mist", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota PROACE VERSO 2.2 D, 150 K, L2 Combi", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Ford C-MAX 1,6i 74kW M/T LPG-nova revize", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota Corolla 1.8 Hybrid GR-Sport Dynamic", ""))
                .isEqualTo("HATCHBACK");
    }

    @Test
    void extractsYearFromFreshTitleWhenStructuredYearIsMissing() throws Exception {
        assertThat(extractYearFromTitle("Kia K4 1,0 T-GDi GPF SPIN (2026)")).isEqualTo(2026);
    }

    @Test
    void keepsPlugInHybridWhenModelAndPlugInAreJoined() throws Exception {
        assertThat(mapElectrifiedFuel("Toyota RAV4 2.5Plug-in Hybrid 4x4 304k"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(mapElectrifiedFuel("Toyota RAV4 2.5Plug-in 4x4 304k"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(mapElectrifiedFuel("Toyota C-HR 2,0 PHEV E-CVT Style"))
                .isEqualTo("PLUGIN_HYBRID");
        assertThat(mapElectrifiedFuel("Toyota C-HR 1.8 Style"))
                .isEqualTo("HYBRID");
        assertThat(mapElectrifiedFuel("Toyota C-HR 2.0Hybrid,CZ,1Maj,Style"))
                .isEqualTo("HYBRID");
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

    @Test
    void repairsToyotaProvereneMojibakeToFinalCzechText() throws Exception {
        assertThat(repairMojibake("\u0139\u00A0koda Fabia 1.2 HTP / 51 kW"))
                .isEqualTo("\u0160koda Fabia 1.2 HTP / 51 kW");
        assertThat(repairMojibake("TSUSHO Mod\u0139\u2122any - Skladov\u0102\u00A9 vozy"))
                .isEqualTo("TSUSHO Mod\u0159any - Skladov\u00E9 vozy");
        assertThat(repairMojibake("Citro\u0102\u00ABn C4 1.2PureTech"))
                .isEqualTo("Citro\u00EBn C4 1.2PureTech");
    }

    @Test
    void repairsFreshToyotaProvereneMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("Uhersk\u0102\u00A9 Hradi\u0139\u02C7t\u00C4\u203A"))
                .isEqualTo("Uhersk\u00E9 Hradi\u0161t\u011B");
        assertThat(repairMojibake("\u0139\u00A0koda Citigo 1.0 MPI 55kW Style"))
                .isEqualTo("\u0160koda Citigo 1.0 MPI 55kW Style");
        assertThat(repairMojibake("Citro\u0102\u00ABn C5 Aircross 1.6PureTech,133kW,AT8,CZ,SHINE"))
                .isEqualTo("Citro\u00EBn C5 Aircross 1.6PureTech,133kW,AT8,CZ,SHINE");
        assertThat(repairMojibake("Opel Zafira 1.6CDTI,88kW,7M\u0102\u00ADst,Ta\u0139\u013En\u0102\u00A9"))
                .isEqualTo("Opel Zafira 1.6CDTI,88kW,7M\u00EDst,Ta\u017En\u00E9");
        assertThat(repairMojibake("Tsusho Praha Pr\u0139\u017Bhonice"))
                .isEqualTo("Tsusho Praha Pr\u016Fhonice");
        assertThat(repairMojibake("Emil Frey ojet\u0102\u00A9 vozy"))
                .isEqualTo("Emil Frey ojet\u00E9 vozy");
        assertThat(repairMojibake("Toyota Aygo 1.0i, \u00C4\u015AR, X-play, automat"))
                .isEqualTo("Toyota Aygo 1.0i, \u010CR, X-play, automat");
        assertThat(repairMojibake("T\u0139\u2122inec"))
                .isEqualTo("T\u0159inec");
        assertThat(repairMojibake("\u0139\u00A0umperk"))
                .isEqualTo("\u0160umperk");
        assertThat(repairMojibake("pouze do vyprod\u0102\u02C7n\u0102\u00AD"))
                .isEqualTo("pouze do vyprod\u00E1n\u00ED");
    }

    @Test
    void repairsCurrentToyotaProvereneMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("Toyota Yaris Cross 1.5 Hybrid 130k - Style - PĹEDPRODEJ NOVINKY - PĹ™edĂˇnĂ­ ZĂˇĹ™Ă­ 2026"))
                .isEqualTo("Toyota Yaris Cross 1.5 Hybrid 130k - Style - PŘEDPRODEJ NOVINKY - Předání Září 2026");
        assertThat(repairMojibake("Toyota PROACE VERSO 2.0 D-4D 130kW L1 Family TaĹľnĂ©"))
                .isEqualTo("Toyota PROACE VERSO 2.0 D-4D 130kW L1 Family Tažné");
        assertThat(repairMojibake("TSUSHO PrĹŻhonice - SkladovĂ© vozy"))
                .isEqualTo("TSUSHO Průhonice - Skladové vozy");
        assertThat(repairMojibake("DolĂˇk PĂ­sek"))
                .isEqualTo("Dolák Písek");
        assertThat(repairMojibake("PRĹ®HONICE"))
                .isEqualTo("PRŮHONICE");
        assertThat(repairMojibake("Toyota C-HR 1.8HEV, \u00C4\u015AR-1m, Style, DPH"))
                .isEqualTo("Toyota C-HR 1.8HEV, \u010CR-1m, Style, DPH");
        assertThat(repairMojibake("Tsusho Praha Pr\u0139\u017Bhonice"))
                .isEqualTo("Tsusho Praha Pr\u016Fhonice");
        assertThat(repairMojibake("Citro\u0102\u00ABn C4 1.2 Puretech"))
                .isEqualTo("Citro\u00EBn C4 1.2 Puretech");
        assertThat(repairMojibake("\u0139\u00A0koda Octavia 1.6 TDI Combi"))
                .isEqualTo("\u0160koda Octavia 1.6 TDI Combi");
        assertThat(repairMojibake("Dol\u0102\u02C7k P\u0139\u2122\u0102\u00ADbram"))
                .isEqualTo("Dol\u00E1k P\u0159\u00EDbram");
        assertThat(repairMojibake("Tsusho Praha Pr\u0139\u017Bhonice"))
                .isEqualTo("Tsusho Praha Pr\u016Fhonice");
        assertThat(repairMojibake("Hradec Kr\u0102\u02C7lov\u0102\u00A9"))
                .isEqualTo("Hradec Kr\u00E1lov\u00E9");
        assertThat(repairMojibake("LOUWMAN \u0102\u0161ST\u0102\u0164 - Nov\u0102\u00A9 a p\u0139\u2122edv\u0102\u02C7d\u00C4\u203Ac\u0102\u00AD vozy"))
                .isEqualTo("LOUWMAN \u00DAST\u00CD - Nov\u00E9 a p\u0159edv\u00E1d\u011Bc\u00ED vozy");
    }

    private String extractCarType(String title, String text) throws Exception {
        Method method = ToyotaProvereneVozyParser.class.getDeclaredMethod("extractCarType", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text);
    }

    private Integer extractYearFromTitle(String title) throws Exception {
        Method method = ToyotaProvereneVozyParser.class.getDeclaredMethod("extractYearFromTitle", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, title);
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

