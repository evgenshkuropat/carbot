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
        assertThat(extractBrand("Seat Tarraco / Skoda Kodiaq 2.0 TDi DSG bez investic", ""))
                .isEqualTo("SEAT");
        assertThat(extractBrand("Skoda Kodiaq / Seat Tarraco 2.0 TDi DSG bez investic", ""))
                .isEqualTo("SKODA");
        assertThat(extractBrand("SEAT ALHAMBRA 2.0TDI 103KW 7MIST TAZNE Z.", "volkswagen sharan"))
                .isEqualTo("SEAT");
        assertThat(extractBrand("PRODAM MAZDU 6 GH VE VYBORNEM STAVU", ""))
                .isEqualTo("MAZDA");
        assertThat(extractBrand("ID.4 1.st MAX 80kwh 150KW MATRIX HEAD UP 107tkm m2021", "peugeot 2008"))
                .isEqualTo("VOLKSWAGEN");
        assertThat(extractBrand("Toyata Yaris 1.5 16V, Edice Y20, 41 tis km", ""))
                .isEqualTo("TOYOTA");
        assertThat(extractBrand("C-Hr 1.8 hybrid", ""))
                .isEqualTo("TOYOTA");
        assertThat(extractBrand("C3, 1,4i, 54 kw", "")).isEqualTo("CITROEN");
        assertThat(extractBrand("Abarth 500 Turbo Cabrio 107 kW 2018 CZ puvod", "")).isEqualTo("ABARTH");
        assertThat(extractBrand("Prodam Fiat Multipla 1.6/16V CNG/2007/6mist", "")).isEqualTo("FIAT");
        assertThat(extractBrand("Focus combi", "skoda octavia")).isEqualTo("FORD");
        assertThat(extractBrand("Lancia Kappa 2.4JTD 10V Klima, Alcantara, Bez koroze, Servis", ""))
                .isEqualTo("LANCIA");
        assertThat(extractBrand("S4 Quattro BSR 402PS 1.majitel koupeno v CR full servis -DPH", ""))
                .isEqualTo("AUDI");
        assertThat(extractBrand("Prugeot 208,1.2,VTi", ""))
                .isEqualTo("PEUGEOT");
        assertThat(extractBrand("Nissan Pulsar 1.2 85kW 2015 CZ", ""))
                .isEqualTo("NISSAN");
        assertThat(extractBrand("508 1.6HDI 82kw", "")).isEqualTo("PEUGEOT");
        assertThat(extractBrand("Prodam Hondu HRV, nejvyssi vybava Advance,2022", ""))
                .isEqualTo("HONDA");
        assertThat(extractBrand("Dacia Bigster journey hybrid 155", ""))
                .isEqualTo("DACIA");
        assertThat(extractBrand("Doblo 1,3 jtd,37tis.km,1.maj.CR,odpocet dph", "skoda octavia"))
                .isEqualTo("FIAT");
    }

    @Test
    void resolvesFuelFromSuzukiBazosTitles() throws Exception {
        assertThat(extractFuelType("Suzuki Vitara 1.6 DDiS AllGrip 4x4")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Suzuki Grand Vitara 2.4 VVT")).isEqualTo("PETROL");
        assertThat(extractFuelType("Suzuki Vitara 1.4 BoosterJet AllGrip Mild-Hybrid r2020")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Suzuki Virara 1.6 Ddis")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Suzuki Alto,1.0i,50kw")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodam Suzuki sx4,1.6")).isEqualTo("PETROL");
        assertThat(extractFuelType("Suzuki Ignis 1.2 PREMIUM 66kW 1.maj.CR")).isEqualTo("PETROL");
    }

    @Test
    void resolvesFuelFromBmwAndAudiBazosTitles() throws Exception {
        assertThat(extractFuelType("Audi S3")).isEqualTo("PETROL");
        assertThat(extractFuelType("Audi A4 B5 1.9 TDI S4 LOOK")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW. 118d")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW 320xd 135kw automat xdrive")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW 530xd E61 LCI M-paket")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW 335i E92 M3 LOOK")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW M3 MANUAL KOMPRESOR")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW Z4 3.0 si MANUAL Coupe")).isEqualTo("PETROL");
        assertThat(extractFuelType("Audi A6 Allroad 235 kW")).isEqualTo("DIESEL");
        assertThat(extractFuelType("AUDI 100 C3 QUATTRO 2.2 100KW 2X UZAVERKA RENOVACE"))
                .isEqualTo("PETROL");
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
        assertThat(extractFuelType("Alfa Romeo Giulia 2.0 Veloce 206kW 2023")).isEqualTo("PETROL");
        assertThat(extractFuelType("Audi A6 Avant 2.0 45 eTFSI 195kW S-tronic")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Audi S6 7.2020 257kw, vybava, krasny stav")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Honda HR-V 1.5 benzin hybrid")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Honda CR-V 2.0 e:HEV Advance AWD")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Honda CR-V 2.0i-MMD Elegance AWD")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Honda Jazz 1.4 61kw. r.2005")).isEqualTo("PETROL");
        assertThat(extractFuelType("Honda Civic 7g, EP2, Sport")).isEqualTo("PETROL");
        assertThat(extractFuelType("Honda Civic Type-R FN2 (2007)")).isEqualTo("PETROL");
        assertThat(extractFuelType("Honda Accord Coupe KUZE AUTOMAT PLYN")).isEqualTo("LPG");
        assertThat(extractFuelType("Prodam Honda City 1,4 73 kw")).isEqualTo("PETROL");
        assertThat(extractFuelType("FIAT 500 1.0 11/2022 DPH 61000km zanovni")).isEqualTo("PETROL");
        assertThat(extractFuelType("Fiat 500 / 0.9 TwinAir / SPORT / 77kW / NAVI")).isEqualTo("PETROL");
        assertThat(extractFuelType("Toyota Yaris Cross, 1.5HEV, Adventure, 4x4")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Peugeot 308 SW 1.2 PT 96 kW 130 Allure CZ DPH")).isEqualTo("PETROL");
        assertThat(extractFuelType("PEUGEOT 301 1.2 60kW rok 2016")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodám Peugeot 301,R.v.2013, 1,6 Nafta.")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Citroen C5 combi,2,2 diesel,125 kW, Webasto")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Fiat Freemont 2.0 MJT AT 4x4 125kW")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Fiat Doblo Maxi 2020 1.6 MJT2 dlouha verze")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Mercedes GLC 350 D 4MATIC, 3.0 V6 nez. topeni")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Mercedes E43 AMG")).isEqualTo("PETROL");
        assertThat(extractFuelType("Mitsubishi Outlander 2.4i+HYBRID 4x4 SERVISKA TAZNE")).isEqualTo("HYBRID");
        assertThat(extractFuelType("MITSUBISHI ECLIPSE CROSS 2.4 PHEV 138kW 4x4-12/2022-49.949KM")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Ford Kuga 2,5PHEV 165KW TitaniumX, Model 023, LED,B&O,vč.DPH")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Mitsubishi lancer evo")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan Primera 1.8 16V 2006")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan Micra 1.0 IG-T LED KLIMA")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan Micra 1.2 59kw r.v.2011")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan 200SX 2,0 16V S14 Racing Edition SR20DET")).isEqualTo("PETROL");
        assertThat(extractFuelType("Seat Leon Cupra 300 ST ACC DCC")).isEqualTo("PETROL");
        assertThat(extractFuelType("Opel Crossland 1.2T 81kW LED LIMITED CARPLAY")).isEqualTo("PETROL");
        assertThat(extractFuelType("Toyota GR86 executive manualni prev. odpocet DPH")).isEqualTo("PETROL");
        assertThat(extractFuelType("Toyota GR Yaris s upravami za skoro 700.000,-")).isEqualTo("PETROL");
        assertThat(extractFuelType("BMW e46 330ci, M Packet, 170KW, 231 hp")).isEqualTo("PETROL");
        assertThat(extractFuelType("Mini Cooper")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodam Toyota Mirai Executive")).isEqualTo("ELECTRIC");
        assertThat(extractFuelType("Renault Twingo Z.E. 60KW rok 12/2023 Urban Night")).isEqualTo("ELECTRIC");
        assertThat(extractFuelType("VW Passat B8 Variant GTE 1.4TSI 160kW DSG - zaruka Autodraft")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("VW Golf 8 GTE 1.4 TSI Hybrid 150kW DSG - zaruka Autodraft")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Skoda Octavia IV 2.0TDI 110KW DSG 2021 STYLE")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Skoda Superb IV 2.0TDI L&K 110kW CZ 2025")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Skoda Octavia 4 1.4TSI iV 150kW DSG Sport")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Dacia Lodgy MPV r.2022 1,3benz 96kw 1.majitel")).isEqualTo("PETROL");
        assertThat(extractFuelType("Pekna Dacia Logan MCV 1.2...16V")).isEqualTo("PETROL");
        assertThat(extractFuelType("Citroen C3 1.2 60 kw r.v 2016 115000 km")).isEqualTo("PETROL");
        assertThat(extractFuelType("Fiat Tipo 2017 Lounge 1.6 E-Torq EVO 81 kW - automat 6st.")).isEqualTo("PETROL");
        assertThat(extractFuelType("VOLVO V90 CROSS COUNTRY ULTIMATE B5 173KW 2022 CZ DPH 1MAJ")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Volvo XC 90 B5 AWD INSCRIPTION")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Volvo XC40 B3 5/2026 PLUS BLACK EDITION , DPH , 465km")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Volvo XC60 R-DESIGN D4 140 kW AWD")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Volvo V40 D2 2.0 88kW 2016 Ocean Race")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Volvo XC70 D5 Summum AWD Aut 136 kW")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Volvo XC90 2.4D5 136KW 4x4 AUT SERVISKA")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Volvo XC60 2.0D AUT 5VALEC FACELIFT VYHREV TAZNE")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Volvo V40 2.0D3 5VALEC MANUAL SERVISKA")).isEqualTo("DIESEL");
        assertThat(extractFuelType("XC90 T8")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("S90, 2,0T,T8,408koni,1.maj.odpocet DPH")).isEqualTo("PLUGIN_HYBRID");
        assertThat(extractFuelType("Alfa Romeo GIULIA 2016 2.2,132kW Quadrifoglio body kit")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Alfa Romeo Stelvio - VELOCE TI,2.2 154KW DPH")).isEqualTo("DIESEL");
        assertThat(extractFuelType("BMW M340D DPH")).isEqualTo("DIESEL");
        assertThat(preferExplicitTitleFuelType("VW T6.1 CALIFORNIA BEACH/110KW/DSG/2020/VIRTUAL/Kuchyn/tazne",
                "PLUGIN_HYBRID")).isNull();
        assertThat(extractFuelType("Prodam Fiat Multipla 1.6/16V CNG/2007/6mist")).isEqualTo("CNG");
        assertThat(extractFuelType("Ford Focus 1.6-16V")).isEqualTo("PETROL");
        assertThat(extractFuelType("FIAT 500, 1,2, 51kW, r.v:2015")).isEqualTo("PETROL");
        assertThat(extractFuelType("Fiat Panda 1.1 nova stk")).isEqualTo("PETROL");
        assertThat(extractFuelType("Fiat Panda 1.2 51kW,servisni knizka, klimatizace, kola")).isEqualTo("PETROL");
        assertThat(extractFuelType("Chevrolet Spark 1,0")).isEqualTo("PETROL");
        assertThat(extractFuelType("Chevrolet aveo 1.4")).isEqualTo("PETROL");
        assertThat(extractFuelType("Chevrolet Orlando 2,0 96kw")).isEqualTo("DIESEL");
        assertThat(extractFuelType("Toyota Sienna AWD 2017 7 mist 8AT tazne")).isEqualTo("PETROL");
        assertThat(extractFuelType("Nissan Pixo 1.0 50 kW Pure Drive klima servis")).isEqualTo("PETROL");
        assertThat(extractFuelType("Peugeot 108 1.0benzin servisni hostorie")).isEqualTo("PETROL");
        assertThat(extractFuelType("Opel Corsa 1.4, rok 2018, najeto 156tkm")).isEqualTo("PETROL");
        assertThat(extractFuelType("Opel Mokka 1,2, Ultimate r.v.2022 naj.30000.-km")).isEqualTo("PETROL");
        assertThat(extractFuelType("Prodám Opel Astra H 1.6 85 kW R. V. 2009")).isEqualTo("PETROL");
        assertThat(extractFuelType("Dacia Duster 1.6 SCe 84Kw 1.majitel 109000km uplny servis")).isEqualTo("PETROL");
        assertThat(extractFuelType("Dacia Dokker 1.3 75kw 1.Maj CR DPH")).isEqualTo("PETROL");
        assertThat(extractFuelType("https://auto.bazos.cz/inzerat/219335979/duster-4x4-12-hybrid.php")).isEqualTo("HYBRID");
        assertThat(extractFuelType("Lexus RX 400h")).isEqualTo("HYBRID");
        assertThat(looksAutomaticHybridTitle("Lexus RX 400h", "HYBRID")).isTrue();
        assertThat(extractFuelType("Toyota Aygo")).isEqualTo("PETROL");
        assertThat(looksAutomaticHybridTitle("Toyota Auris 1,8HSD 73kW", "HYBRID")).isTrue();
        assertThat(extractFuelType("Volvo C30 T5- Vyjimecna motorizace 227HP")).isEqualTo("PETROL");
        assertThat(extractFuelType("Civic 1.8l 103kw GT")).isEqualTo("PETROL");
    }

    @Test
    void resolvesCarTypesFromBazosTitles() throws Exception {
        assertThat(extractCarType("Opel Insignia Country Tourer 4x4 tazne manual", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Antara", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Seat Leon Cupra 300 ST ACC DCC", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Seat Leon1.5 TSi 96kW 1majitel CR Xcellence", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Seat Leon ST 1.2 TSI, 81kW, r2017", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Seat Altea XL 1.6 TDI 77 kW Automat", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Seat ibiza", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Seat Toledo 1.2 TSI 66 kW, 2016, 111 tis. km", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("SEAT IBIZA 1.0 MPi 55kW KOMBI 2016", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Seat IBIZA combi, 1.2 TSI, 77kW, NOVA STK, TOP STAV", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Suzuki Jimny 1.3 i 2015", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("suzuki jimny 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Samurai 1.3", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Ignis 1.2 Spajacie z. za karavan, Bluetooth", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("SUZUKI SPLASH 1,2 AUTOMAT NOVA STK", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Prodam Suzuki sx4,1.6", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Prodam Suzuki S X4 1.6", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Virara 1.6 Ddis", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Suzuki Alto,1.0i,50kw", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Suzuki Kizashi 4X4", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Audi S3", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Alfa Romeo 75 2.0 Twinspark", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("AR 159 1.75TBi", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Alfa 159Ti 2.4 JTDm 154kw", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Alfa Romeo 156 SW 2.4 JTD 20v TI", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Alfa Romeo Guilietta", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Volvo C30 T5- Vyjimecna motorizace 227HP", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Honda Accord VIII Tourer 2.2 i-DTEC 110 kW", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Hyundai i30N Performance", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Mini Cooper 1.5 i 2018 F 56", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("BMW 325i e91", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("BMW 330 xD", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW F36 430d 258Hp GC 05/2016 original M-Paket", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW 420D Xdrive 2018", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("BMW 6 GT xDrive M-Paket", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW F10 523i, 185tis, 3.0, 150kW, automat", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW 530D XDRIVE 210kW FACELIFT 2021 AUTOMAT / HEADup VIRTUAL", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW 530d UVEDENA CENA BEZ DPH", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW 750 XDRIVE 400 PS LASER LIGHT M-PACK", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW M340D DPH", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Audi A6 2.0 TDI AVANT Ultra S-tronic 2015", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("A6C7 avant", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Audi a4b6 2.5tdi V6 120kw", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("S4 Quattro BSR 402PS 1.majitel koupeno v CR full servis -DPH", "", ""))
                .isEqualTo("SEDAN");
        assertThat(extractCarType("Audi S6 Quattro UVEDENA CENA BEZ DPH", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("AUDI 100 C3 QUATTRO 2.2 100KW 2X UZAVERKA RENOVACE", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Audi S6 Avant 55 TDI Nelakovano Nebourano Servis Audi", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Audi A3 / 2018 / 1,6 / 85 kw", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Audi A4 B5 1.9 TDI S4 LOOK", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("BMW M3 MANUAL KOMPRESOR", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("BMW Z4 3.0 si MANUAL Coupe", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("BMW 2, F45, Active Tourer, 225i xDrive LUXURY LINE", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Citroen C 3 1.5 HDi, Edice Origins Since 1919", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Citroen c-elysee 1.2", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Prodam Citroen Jumpy 2.0 HDI Multispace 9.mist", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Lancia Kappa 2.4JTD 10V Klima, Alcantara, Bez koroze, Servis", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Lancia Ypsilon Gold 1.2i", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Fiat Dobló 1,6Jtd MAXI klima+5dveri+CR+64000km", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat Talento Kombi 1.6turbo 107kw,novy motor 8mist,zaves", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat Croma 1,9jtd AUTOMAT 2009", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Fiat Stilo 1.9jtd dovoz, 100tkm, servisni knizka top stav", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Fiat500", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Fiat 500c 1.2 Lounge 2015", "", "")).isEqualTo("CABRIO");
        assertThat(extractCarType("Fiat 500X - 1.0 FireFly - edice MIRROR", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Prodam Fiat Multipla 1.6/16V CNG/2007/6mist", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat FIORINO QUBO 1.4 - DPH - pouze 154000 km", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Fiat Bravo 1,6 JTD, 2008", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Citroen DS4 Exclusive", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Citroen eC4 zaruka elektro", "", "https://auto.bazos.cz/inzerat/219841903/citroen-ec4-zaruka.php")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("DACIA STEPWAY 1,0 i 66 KW TOP STAV 2017", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Dacia Bigster journey hybrid 155", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("HONDA ACCORD TOURER VII EXECUTIVE 2.0 i-VTEC", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Honda Accord kombi 2,0i-Vtec slusny stav servis STK", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Honda Accord coupe", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("Honda Acoord 8G 2.0 I-VTEC", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Prodam Honda City 1,4 73 kw", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Honda Covic 2.2 CTDi,103kw, nova STK.", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Honda N-Box Custom 3/2016 136t km JDM", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Ford Focus Tunier 2014", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("FUSION FACELIFT,1.4 16V 59KW,ROK 2008", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Ford f-150 5.4 lpgrv. 2010, double cab,2 m korba", "", "")).isEqualTo("PICKUP");
        assertThat(extractCarType("HONDA CIVIC TOURER 1.6i DTEC 2016 KAMERA", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Honda F-RV 1,8 V-Tec 108kW 6-mist", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Chevrolet Express Limited SE 2500 6.0", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Chevrolet Trailblazer 4.2i,201kw,/LPG", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Opel Astra J Sports Tourer 1.4i Turbo103Kw r.v.10/2015", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Astra J combi 1,7 CDTi 92kW", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Astra K 1,6 CDTI 81kw sport Tourer, Innovation", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Astra K 1,6 CDTI 81kw 2016 ST, Innovation", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Opel Insignia Sport Taurer", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("OPEL ASTRA SPORTS TOUER 1.6CDTI 81KW EDITION", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Nissan Primera P12 2.2D", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Nissan Pulsar 1.2 85kW 2015 CZ", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Nissan Elgrand 3.5 V6", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("OPEL AMPERA PLUGIN-HYBRID ELEKTRO", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Opel Crossland X 1.2i 81kw Inovation", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("OPEL VECTRA C 2.2i 16V EDICE", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Prodame Peugeot Travaller", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Peugeot Traveller 2.0 Blue-HDi Allure L2", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Peugeot 108 1.0benzin servisni hostorie", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("PEUGEOT 301 1.2 60kW rok 2016", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Peugeot 405 SRI 2,0", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Peugeot 4008 1.8HDI 4x4 MANUAL", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota Corolla ST 1.8 HEV 103kW e-CVT,2024,35tkm", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Toyota Rav 4 2.0D 85Kw Nova STK, 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota CH-R 1.8 hybrid", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota Aoris", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Toyota Avensis T25 2.0/93kw/D4D", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Avensis 1.8i WG Sol", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Toyota and Cruiser HDJ 80 Expedicni", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota Camry Executive HYBRID", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Yaris Cross, 1.5HEV, Adventure, 4x4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota 4runner - SPECIAL z mise OSN", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Toyota Prius Plus 7mist+LPG 2013", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota Sienna AWD 2017 7 mist 8AT tazne", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Lexus IS 220d", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Toyota Starlet", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Toyota bZ4X 2023 - 19.000km / odpocet DPH", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Hyundai SantaFe 4 x 4", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Hyundai IX 20", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Hyundai Staria 2,2 CRD 149 Kw", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Toyota Aygo 1.0VVT-i 50kw 4/2013", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Prodám Toyota Mirai Executive", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Škoda Yeti, 2.0 TDi 4X4 Outdoor Nové Rozvody", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("VW ID.3 Pro 150kW IQ.Lights SOH 95,7%", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("VW Passat B8 Varian TDI 110kW DSG", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("VW Arteon SB 2.0 TDI 110kW DSG R-Line", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("VW GOLF PLUS 1,4 TSi 90 KW TOP STAV", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("VW T6.1 CALIFORNIA BEACH/110KW/DSG/2020/VIRTUAL/Kuchyn/tazne", "", ""))
                .isEqualTo("MINIVAN");
        assertThat(extractCarType("Scirocco 2.0 TSI DSG 155kw r 2012", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("Volkswagen UP 1.0MPI KLIMA", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Volvo S80 2.4D5 120 kW Klima Tempomat CR", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Volvo S90 B5 AWD 173 kW 6/2023 Inscription CR 1. majitel", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Volvo C70 2.0d 5valec automat 130kW", "", "")).isEqualTo("CABRIO");
        assertThat(extractCarType("Volvo v 90", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Audi A2 1.4 TDI STK 2028", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Renault Talisman 1.6dCI MANUAL VYHREV TAZNE", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Renault Alaskan", "", "")).isEqualTo("PICKUP");
        assertThat(extractCarType("Renault Laguna 2", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("RENAULT TWINGO 1.0i 51kW 2018 POUZE 15 603KM", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Renault Tvingo 1.0i editovana kolekce", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("Cupra Tavascan Endurance Electric 210kW", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("Kia Magentis 2.0crdi 103kw Top Stav Nova STK", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Kia Stinger GT 3.3 T-GDI 4WD 1. majitel", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Optima", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Kia Venga", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("Kia Carnival 2.9 Crdi 136Kw 7mist", "", "")).isEqualTo("MINIVAN");
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
        assertThat(extractCarType("Mercedes-Benz B 250e Progressive Plug-in Hybrid 2021", "", "")).isEqualTo("MINIVAN");
        assertThat(extractCarType("MERCEDES E 220D 4MATIC COMBI PUVOD CR", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes-Benz C 220 d T AMG Night Paket, 162 kW, 2023", "", "")).isEqualTo("WAGON");
        assertThat(extractCarType("Mercedes-Benz 126.500 SEC AMG Paket", "", "")).isEqualTo("COUPE");
        assertThat(extractCarType("PRODAM MAZDU 6 GH VE VYBORNEM STAVU", "", "")).isEqualTo("SEDAN");
        assertThat(extractCarType("Mazda Tribute 3.0 4x4 nova STK", "", "")).isEqualTo("SUV");
        assertThat(extractCarType("AUDI A1 1.2 TFSI 2012", "", "")).isEqualTo("HATCHBACK");
        assertThat(extractCarType("C5 2.2HDI 16V BREAK D. rv.11.2006", "", "")).isEqualTo("WAGON");
    }

    @Test
    void resolvesAlfaRomeoTransmissionAndSkipsPartTitles() throws Exception {
        assertThat(extractTransmission("Alfa Romeo 156 2.0 JTS 16V Selespeed Distinctive Funny car")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Alfa Romeo Stelvio 2.0 Turbo 16V AT8-Q4 Veloce T")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Toyota Corolla ST 1.8 HEV 103kW e-CVT,2024,35tkm")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Honda CR-V 2.0 e:HEV Advance AWD")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Honda CR-V 2.0i-MMD Elegance AWD")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Honda Jazz 1.4 i-VTEC, r.v. 2010, i-Shift")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Honda CRV 2,2 i-DTEC Automat, odpocet DPH")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("VOLVO V90 2,0 221kW B6 AWD 4x4 INSCRIPTION Auto 2021 CR DPH"))
                .isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Toyota Prius 30 2011 lpg 1.8")).isEqualTo("AUTOMATIC");
        assertThat(looksAutomaticHybridTitle("Ford Kuga 2,5PHEV 165KW TitaniumX", "PLUGIN_HYBRID")).isTrue();
        assertThat(looksAutomaticHybridTitle("Ford Kuga 2,5 140kW 4x4 AWD ST-LINE", "HYBRID")).isTrue();
        assertThat(looksAutomaticHybridTitle("CR-V r. 2022 2.0 hybrid 4/4", "HYBRID")).isTrue();
        assertThat(looksAutomaticHybridTitle("TOYOTA C-HR 1.8 Hybrid EXECUTIVE-LED-KAMERA-ACC-CR", "HYBRID")).isTrue();
        assertThat(looksAutomaticHybridTitle("BMW X5 xDrive 45e 290kW 2020", "HYBRID")).isTrue();
        assertThat(looksAutomaticHybridTitle("Dacia Bigster journey hybrid 155", "HYBRID")).isTrue();
        assertThat(looksLikelyFalseManual("BMW X5 xDrive 45e 290kW 2020", "MANUAL")).isTrue();
        assertThat(looksLikelyFalseManual("TOYOTA C-HR 1.8 Hybrid EXECUTIVE-LED-KAMERA-ACC-CR", "MANUAL")).isTrue();
        assertThat(extractTransmission("Ford Kuga ST Line 1,5 110 kW benzin 6-ti st.mech.")).isEqualTo("MANUAL");
        assertThat(extractTransmission("Citroen Berlingo 1.5 BlueHDi 130S&S MAN 6 SHINE")).isEqualTo("MANUAL");
        assertThat(extractTransmission("Citroen Berlingo 1.6 BlueHDI XTR 100 MAN")).isEqualTo("MANUAL");
        assertThat(extractTransmission("Hyundai i30 Kombi 1.6 CRDi 85kW DCT (2018)")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("BMW 750 XDRIVE 400 PS LASER LIGHT M-PACK")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Dacia Duster TCe 150 EDC, TOP, DPH")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Skoda Octavia 4 combi RS 2.0TDi,147kW,DSG,4x4")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Skoda Octavia IV 2.0TDI 110KW DSG•2021•STYLE")).isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Ĺ koda Octavia 4 RS combi 2.0TSi,180kW,DSG,Canton,22TKM"))
                .isEqualTo("AUTOMATIC");
        assertThat(extractTransmission("Ĺ koda Octavia 4 combi RS 2.0TDi,147kW,DSG,TaĹľnĂ©,Panorama,DPH"))
                .isEqualTo("AUTOMATIC");
        assertThat(looksLikelyFalseAutomatic("PEUGEOT 207 1.4 i BENZIN 70 kW NOVE ROZVODY", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Peugeot 308 1.6HDI 88KW 9/2015 LED NAVIGACE P. SERVIS", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Octavia III 2,0TDi 110KW Edition + NAVI tempomat ALU STK", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("naftova Skoda Octavia 3 SCOUT 2.0 Tdi 135kW 136000km 4x4", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Alfa Romeo GTV 2.0 V6 Turbo - vyjimecny stav", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Alfa Romeo Brera 2.4jtd 147kw", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Alfa Romeo GT Coupe 1.8 103kw,Rok 2007,176tkm,Klima,Nova STK", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Alfa Romeo Giulia 2.2 jTDm Super Business plus Aut 8st.", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Skoda Octavia IV 2.0TDI 110KW DSG•2021•STYLE", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Fabia, 1.2 TSI Style Combi TAZNE, Aut. klima, Vyhr. sedadla", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Skoda Rapid, 1.2 TSI SPACEBACK Sport Aut. klima, Vyhr. sedadla", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("SKODA YETI 2.0 Tdi 81 kW 4x4 ALU KOLA, TAZNE, KLIMA", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Dacia Duster 1.6 16V - BENZIN - 4X4", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Dacia Duster TCe 150 EDC, TOP, DPH", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Dacia Sandero Stepway 1.0i 67KW LPG Keyless Vyhrev Kamera", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Dacia Jogger 1.0 LPG+benzin xtreme", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Dacia Dokker 1.6 SCe 75kW, 2019", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Fiat Panda 1,1i,puvod CZ,1.majitel,86tkm", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Toyota Avensis T25 2.0/93kw/D4D", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Toyota Auris, 1.6i / EXECUTIVE CR", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Volkswagen Golf 7 Variant 1.4 TSI 90 kW", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("VW Golf 8 Variant 2.0 TDI 110kW DSG", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Opel Zafira B", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Peugeot Partner Tepee 1,6 Hdi 8v 68kw", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Renault Clio IV 2013, 1.2 16V", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Citroen Berlingo 1.6 HDI 84KW", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Seat Leon ST 1.4 TSI 92 kW FR", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Seat Leon 1.4 TSI 92 kW", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("SEAT LEON FR 2.0 TDI 110KW DSG", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Seat Alhambra 2.0tdi 103kw Style 2012", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Seat Alhambra 2.0tdi 103kw DSG", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Suzuki Samurai 1.9d. SLEVA", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Vitara 1.6VVT 88kw 4x4 AllGrip Kamera, Tazne, 2016 nove v CR", "AUTOMATIC")).isTrue();
        assertThat(looksLikelyFalseAutomatic("Suzuki Vitara 1.4 4x4 AUTOMAT ALLGRIP SPORT-KAMERA-LED", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("SUZUKI VITARA 1.6 VVT 4X4 ALLGRIP AUTOMAT-KAMERA-TAZNE 17", "AUTOMATIC")).isFalse();
        assertThat(looksLikelyFalseAutomatic("Suzuki SX4 1.9 DDiS 88kW 4x4 klima Nova STK", "AUTOMATIC")).isTrue();
        assertThat(correctLikelyNoisyFuel("Toyota Sienna AWD 2017 7 mist 8AT tazne", "DIESEL")).isEqualTo("PETROL");
        assertThat(correctLikelyNoisyFuel("Dacia Sandero Stepway", "DIESEL")).isNull();
        assertThat(correctLikelyNoisyFuel("Navara D22 Kingcab 98kw", "PETROL")).isNull();
        assertThat(correctLikelyNoisyFuel("Opel Grandland X 1 majitel servis", "LPG")).isNull();
        assertThat(correctLikelyNoisyFuel("Hyundai Tucson N-Line 2025 TOP STAV", "HYBRID")).isNull();
        assertThat(correctLikelyNoisyFuel("Honda HR-V 1.5 benzin hybrid", "HYBRID")).isEqualTo("HYBRID");
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
        assertThat(looksNonCarListing("Strecha Toyota MR2, pasy, plasty do masky", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Auto pro vozickare/ZTP/auto s rampou", "", "", "")).isTrue();
        assertThat(looksNonCarListing("Honda CBX 1000", "", "https://auto.bazos.cz/inzerat/218943387/elektricke-auto.php", "")).isTrue();
        assertThat(looksNonCarListing(
                "S4 Quattro BSR 402PS 1.majitel koupeno v CR full servis -DPH",
                "",
                "https://auto.bazos.cz/inzerat/219539804/s4-quattro-1majitel-koupeno-v-cr-full-servis-odpocet-dph.php",
                ""))
                .isFalse();
        assertThat(looksNonCarListing(
                "Scirocco 2.0 TSI DSG 155kw r 2012",
                "",
                "https://auto.bazos.cz/inzerat/220075516/scirocco-20-tsi-dsg-155kw-r-2012.php",
                ""))
                .isFalse();
        assertThat(looksNonCarListing(
                "Suzuki Kizashi 4X4",
                "",
                "https://auto.bazos.cz/inzerat/220579760/suzuki-kizashi-4x4.php",
                ""))
                .isFalse();
        assertThat(looksNonCarListing(
                "C-Hr 1.8 hybrid",
                "",
                "https://auto.bazos.cz/inzerat/220804547/c-hr-18-hybrid.php",
                ""))
                .isFalse();
    }

    @Test
    void rejectsBareBrandTitlesButKeepsRealModels() throws Exception {
        assertThat(looksSuspiciousListing("Alfa romeo", "")).isTrue();
        assertThat(looksSuspiciousListing("Seat", "")).isTrue();
        assertThat(looksSuspiciousListing("Alfa Romeo 159", "")).isFalse();
        assertThat(looksSuspiciousListing("SKODA FABIA 3 1,0MPi 44kW Koup.CR,1.majitel,Serv. kniha,2019", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("SKODA SCALA 1,0TSi 70kW Koup.CR,1.majitel,LED,2022,119tkm", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("SKODA KAMIQ 1,0TSi 81kW Koup.CR,1.majitel,TAZNE,2022,DPH", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("SKODA KODIAQ 1,5TSI 110kW SPORTLINE TAZNE Koup.CR,1.majitel", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("VW ARTEON 2,0TDi 110kW DSG ELEGANCE CR 2022 NYNI PO SERVISE", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("VW ARTEON 2,0TDi 110kW DSG ELEGANCE ÄŚR 2022 NYNĂŤ PO SERVISE", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("VW PASSAT 2,0TDi 110kW BUSINESS DSG Koup.CR,1.majitel,2023", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("VW T-ROC 1,5TSi 110kW MARATON Koup.CR,TAZNE,Vyhr.volant,2022", "rezervace")).isFalse();
        assertThat(looksSuspiciousListing("VW T-ROC 1,5TSi 110kW MARATON Koup.CR,TAZNE,Vyhr.volant,2022", "na splatky bez registru")).isFalse();
        assertThat(looksSuspiciousListing("VW T-ROC 1,5TSi 110kW na splatky", "")).isTrue();
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
        assertThat(looksTitleUrlMismatch(
                "Citroen C4 zaruka elektro",
                "https://auto.bazos.cz/inzerat/219841903/citroen-ec4-zaruka.php"))
                .isFalse();
        assertThat(looksBrandMismatch(
                "Fiat Tipo 2017 Lounge 1.6 E-Torq EVO 81 kW - automat 6st.",
                "https://auto.bazos.cz/inzerat/220183066/alfa-romeo-giulietta-2015-14-turbo-turismo-125kw.php"))
                .isTrue();
        assertThat(looksBrandMismatch(
                "Lancer 2.0did sedan (vw tdi) 103kw, 1. majitel",
                "https://auto.bazos.cz/inzerat/219696461/lancer-20did-sedan-vw-tdi-103kw-1-majitel.php"))
                .isFalse();
        assertThat(looksBrandMismatch(
                "Fiat 500 ABARTH UVEDENA CENA BEZ DPH MOZNE SPLATKY",
                "https://auto.bazos.cz/inzerat/220047180/fiat-500-abarth.php"))
                .isFalse();
        assertThat(looksBrandMismatch(
                "Seat Tarraco / Skoda Kodiaq 2.0 TDi DSG bez investic",
                "https://auto.bazos.cz/inzerat/220352139/seat-tarraco-skoda-kodiaq-20-tdi-dsg-bez-investic.php"))
                .isFalse();
        assertThat(looksModelUrlMismatch(
                "RENAULT TWINGO 1,2 16v CTYRVAL",
                "https://auto.bazos.cz/inzerat/219681639/renault-clio-12-16v-ctyrval.php"))
                .isTrue();
        assertThat(looksModelUrlMismatch(
                "HYUNDAI i10 OBSAH 1,1i, KLIMA",
                "https://auto.bazos.cz/inzerat/220273544/hyundai-i-10-obsah-11i-klima.php"))
                .isFalse();
        assertThat(looksModelUrlMismatch(
                "HYUNDAI IX20 1.4 i BENZIN",
                "https://auto.bazos.cz/inzerat/219505357/hyundai-ix-20-16-i-benzin.php"))
                .isFalse();
    }

    @Test
    void rejectsExplicitMotorFailureFromFreshLogs() throws Exception {
        assertThat(looksBrokenOrForPartsListing("Opel Astra SW 1.5CDTI DPH CR Motor k.o", ""))
                .isTrue();
        assertThat(looksBrokenOrForPartsListing(
                "Hyundai Ioniq 5, STYLE 77,4 kWh SOH 96,3%; 800V; cerpadlo",
                "vadny dil v doporucenem inzeratu"))
                .isFalse();
        assertThat(looksBrokenOrForPartsListing(
                "HYUNDAI IX55 CRDi 4WD koupeno v CR, jen 167t.KM",
                "nefunkcni polozka v okolnim textu"))
                .isFalse();
        assertThat(looksBrokenOrForPartsListing(
                "Volvo S90 B5 AWD 173 kW 6/2023 Inscription CR 1. majitel",
                "nefunkcni polozka v okolnim textu"))
                .isFalse();
        assertThat(looksBrokenOrForPartsListing(
                "BMW 520d xDrive 2012 135kw",
                "nepojizdny v doporucenem inzeratu"))
                .isFalse();
        assertThat(looksBrokenOrForPartsListing(
                "Fiat 500, 1.2i, CR, PO SERVISU",
                "vadny dil v doporucenem inzeratu"))
                .isFalse();
        assertThat(looksBrokenOrForPartsListing(
                "Fiat 500, 1.2i, ÄŚR, PO SERVISU",
                "vadny dil v doporucenem inzeratu"))
                .isFalse();
    }

    @Test
    void correctsFreshRenaultAndGenericListingSignals() throws Exception {
        assertThat(correctLikelyNoisyFuel("1.Majitel Renault Kangoo 1.2...2017", "DIESEL"))
                .isEqualTo("PETROL");
        assertThat(extractTransmission("RENAULT SCENIC 1.5DCi MANUĂL. ALU"))
                .isEqualTo("MANUAL");
        assertThat(looksSuspiciousListing("prodam avto", "")).isTrue();
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
                "Prodam Citroen Jumpy 2.0 HDI Multispace 9.mist",
                "",
                "https://auto.bazos.cz/inzerat/219797616/prodam-citroen-jumpy-20-hdi-multispace-9mist.php"))
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

        assertThat(looksCommercialVehicle(
                "NISSAN NT400 CABSTAR SKLOPKA 3.0 dci 150 PS",
                "",
                "https://auto.bazos.cz/inzerat/218825089/nissan-nt400-cabstar-sklopka.php"))
                .isTrue();

        assertThat(looksCommercialVehicle(
                "Nakladni Berlingo",
                "",
                "https://auto.bazos.cz/inzerat/218863810/nakladni-berlingo.php"))
                .isTrue();

        assertThat(looksCommercialVehicle(
                "Fiat Doblo Cargo 1,4 70kW-L1H1-2013 2. maj. CR KLIMA-DPH",
                "",
                "https://auto.bazos.cz/inzerat/219715582/fiat-doblo-cargo-14-70kw-l1h1-2013-2-maj-cr-klima-dph.php"))
                .isTrue();

        assertThat(looksCommercialVehicle(
                "Fiat FIORINO QUBO 1.4 - DPH - pouze 154000 km",
                "",
                "https://auto.bazos.cz/inzerat/219170188/fiat-fiorino-qubo-14.php"))
                .isFalse();

        assertThat(looksCommercialVehicle(
                "Mazda Tribute 3.0 4x4 nova STK",
                "",
                "https://auto.bazos.cz/inzerat/219554120/mazda-tribute-30-4x4-nova-stk.php"))
                .isFalse();

        assertThat(looksCommercialVehicle(
                "BMW X 5 3.0 D, 2021 rok, 93 tisic najeto",
                "",
                "https://auto.bazos.cz/inzerat/219905143/bmw-x-5-30-d-2021-rok-93-tisic-najeto.php"))
                .isFalse();

        assertThat(looksCommercialVehicle(
                "BMW 545i V8 MANUAL Mpaket + LPG, sport. podvozek BILSTEIN",
                "",
                "https://auto.bazos.cz/inzerat/220390449/bmw-545i-v8-manual-mpaket-lpg.php"))
                .isFalse();
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
        assertThat(extractYear("Citroen C5 Aircross 1,5 HDI, 72tis.km,r.v.02/22", ""))
                .isEqualTo(2022);
    }

    @Test
    void repairsBazosMojibakeBeforeOutput() throws Exception {
        assertThat(repairMojibake("Ford Kuga 2,0TDCi 140KW 4x4 Nav,Temp,Winterpaket,v\u00C4\u0164. DPH"))
                .isEqualTo("Ford Kuga 2,0TDCi 140KW 4x4 Nav,Temp,Winterpaket,vč. DPH");
        assertThat(repairMojibake("Plze\u0139\u0088-jih")).isEqualTo("Plzeň-jih");
        assertThat(repairMojibake("Honda Jazz 1.2 i 66Kw Nov\u0102\u02C7 STK 65 tkm"))
                .isEqualTo("Honda Jazz 1.2 i 66Kw Nová STK 65 tkm");
        assertThat(repairMojibake("Hyundai i20 1.0 T-GDI 74kW 63tkm - z\u0102\u02C7ruka Autodraft"))
                .isEqualTo("Hyundai i20 1.0 T-GDI 74kW 63tkm - záruka Autodraft");
        assertThat(repairMojibake("Hyundai i30 kombi 1.5T-GDI 117kw|N-LINE|2022|114tkm|Z\u0102\u0081RUKA"))
                .isEqualTo("Hyundai i30 kombi 1.5T-GDI 117kw|N-LINE|2022|114tkm|ZÁRUKA");
    }

    @Test
    void repairsSingleEncodedBazosMojibakeBeforeOutput() throws Exception {
        assertThat(repairMojibake("PlzeĹ-jih"))
                .isEqualTo("Plzeň-jih");
        assertThat(repairMojibake("Audi Q3 1.4TFSI 110KW MANUĂL LED SENZORY SERVISKA TAĹ˝NĂ‰"))
                .isEqualTo("Audi Q3 1.4TFSI 110KW MANUÁL LED SENZORY SERVISKA TAŽNÉ");
        assertThat(repairMojibake("MladĂˇ Boleslav"))
                .isEqualTo("Mladá Boleslav");
        assertThat(repairMojibake("PĹ™erov"))
                .isEqualTo("Přerov");
    }

    @Test
    void repairsFreshBazosMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("Doma\u0139\u013Elice"))
                .isEqualTo("Doma\u017Elice");
        assertThat(repairMojibake("Prod\u0102\u02C7m Nissan Micra 1.2i automat"))
                .isEqualTo("Prod\u00E1m Nissan Micra 1.2i automat");
        assertThat(repairMojibake("P\u0139\u2122\u0102\u00ADbram"))
                .isEqualTo("P\u0159\u00EDbram");
        assertThat(repairMojibake("Hodon\u0102\u00ADn"))
                .isEqualTo("Hodon\u00EDn");
        assertThat(repairMojibake("\u0102\u0161st\u0102\u00AD nad Labem"))
                .isEqualTo("\u00DAst\u00ED nad Labem");
        assertThat(repairMojibake("\u0139\u00A0umperk"))
                .isEqualTo("\u0160umperk");
        assertThat(repairMojibake("Krom\u00C4\u203A\u0139\u2122\u0102\u00AD\u0139\u013E"))
                .isEqualTo("Krom\u011B\u0159\u00ED\u017E");
        assertThat(repairMojibake("OPEL ASTRA 1.6 CDTI 81 kW | 2019 | TA\u0139\u02DDN\u0102\u2030"))
                .isEqualTo("OPEL ASTRA 1.6 CDTI 81 kW | 2019 | TA\u017DN\u00C9");
    }

    @Test
    void repairsJuneBazosMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("PEUGEOT 207 1.4 i BENZ\u0102\u0164N 70 kW NOVE ROZVODY"))
                .isEqualTo("PEUGEOT 207 1.4 i BENZ\u00CDN 70 kW NOVE ROZVODY");
        assertThat(repairMojibake("Brunt\u0102\u02C7l"))
                .isEqualTo("Brunt\u00E1l");
        assertThat(repairMojibake("\u00C4\u015AR"))
                .isEqualTo("\u010CR");
        assertThat(repairMojibake("P\u0139\u2122\u0102\u00ADbram"))
                .isEqualTo("P\u0159\u00EDbram");
        assertThat(repairMojibake("VYH\u0139\u0098EV"))
                .isEqualTo("VYH\u0158EV");
        assertThat(repairMojibake("TA\u0139\u02DDN\u0102\u2030 V\u0102\u0165H\u0139\u0098EV"))
                .isEqualTo("TA\u017DN\u00C9 V\u00DDH\u0158EV");
        assertThat(repairMojibake("M\u0102\u00A9gane"))
                .isEqualTo("M\u00E9gane");
        assertThat(repairMojibake("\u0139\u00A0umperk"))
                .isEqualTo("\u0160umperk");
        assertThat(repairMojibake("K\u0139\u00AE\u0139\u02DDE").codePoints().toArray())
                .containsExactly('K', 0x016E, 0x017D, 'E');
        assertThat(repairMojibake("\u0102\u0161st\u0102\u00AD nad Orlic\u0102\u00AD"))
                .isEqualTo("\u00DAst\u00ED nad Orlic\u00ED");
    }

    @Test
    void repairsCurrentBazosMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("Ĺ koda Superb III combi 2.0 TDi,147kW,DSG,4x4,Sportline,Webas"))
                .isEqualTo("Škoda Superb III combi 2.0 TDi,147kW,DSG,4x4,Sportline,Webas");
        assertThat(repairMojibake("Ĺ koda Superb III combi 2.0 TDi,147kW,DSG,4x4,Sportline,Webas"))
                .isEqualTo("Škoda Superb III combi 2.0 TDi,147kW,DSG,4x4,Sportline,Webas");
        assertThat(repairMojibake("Ĺ koda Enyaq 80iV 82kWH,150kW,LED,TaĹľnĂ©,1.maj,NezĂˇvislĂˇ klima"))
                .isEqualTo("Škoda Enyaq 80iV 82kWH,150kW,LED,Tažné,1.maj,Nezávislá klima");
        assertThat(repairMojibake("Rychnov nad KnÄ›Ĺľnou"))
                .isEqualTo("Rychnov nad Kněžnou");
        assertThat(repairMojibake("Toyota Proace Verso 2.0 D-4D, 9 MĂŤST, LONG"))
                .isEqualTo("Toyota Proace Verso 2.0 D-4D, 9 MÍST, LONG");
        assertThat(repairMojibake("ÄŚeskĂ© BudÄ›jovice"))
                .isEqualTo("České Budějovice");
        assertThat(repairMojibake("JiÄŤĂ­n"))
                .isEqualTo("Jičín");
        assertThat(repairMojibake("UherskĂ© HradiĹˇtÄ›"))
                .isEqualTo("Uherské Hradiště");
        assertThat(repairMojibake("ProstÄ›jov"))
                .isEqualTo("Prostějov");
    }

    @Test
    void repairsJulyBazosMojibakeFromLogs() throws Exception {
        assertThat(repairMojibake("Citro\u0102\u00ABn C4 II 1.6 VTi (88 kW) \u00E2\u20AC\u201C 2011, 134 000km"))
                .isEqualTo("Citro\u00EBn C4 II 1.6 VTi (88 kW) \u2013 2011, 134 000km");
        assertThat(repairMojibake("Citroen C4 Picasso 1.6 HDi - po servise za 15.500,- K\u00C4\u0164"))
                .isEqualTo("Citroen C4 Picasso 1.6 HDi - po servise za 15.500,- K\u010D");
        assertThat(repairMojibake("CITRO\u0102\u2039N SPACETOURER,2.0 HDI,130 KW,EAT8,8 M\u0102\u0164ST"))
                .isEqualTo("CITRO\u00CBN SPACETOURER,2.0 HDI,130 KW,EAT8,8 M\u00CDST");
        assertThat(repairMojibake("Berlingo 1,5hdi,84tis.km,1.maj.\u00C4\u015Ar,TOP stav, DPH"))
                .isEqualTo("Berlingo 1,5hdi,84tis.km,1.maj.\u010Cr,TOP stav, DPH");
        assertThat(repairMojibake("B\u0139\u2122eclav"))
                .isEqualTo("B\u0159eclav");
        assertThat(repairMojibake("Hradec Kr\u0102\u02C7lov\u0102\u00A9"))
                .isEqualTo("Hradec Kr\u00E1lov\u00E9");
        assertThat(repairMojibake("Fr\u0102\u00BDdek - M\u0102\u00ADstek"))
                .isEqualTo("Fr\u00FDdek - M\u00EDstek");
        assertThat(repairMojibake("D\u00C4\u203A\u00C4\u0164\u0102\u00ADn"))
                .isEqualTo("D\u011B\u010D\u00EDn");
        assertThat(repairMojibake("Jind\u0139\u2122ich\u0139\u017Bv Hradec"))
                .isEqualTo("Jind\u0159ich\u016Fv Hradec");
        assertThat(repairMojibake("BENZ\u0102\u0164N,TA\u0139\u02DDN\u0102\u2030,V\u0102\u0165ROBY"))
                .isEqualTo("BENZ\u00CDN,TA\u017DN\u00C9,V\u00DDROBY");
        assertThat(repairMojibake("Alfa Romeo Mito 1.4 benz\u0102\u00ADn 166tkm"))
                .isEqualTo("Alfa Romeo Mito 1.4 benz\u00EDn 166tkm");
        assertThat(repairMojibake("Vset\u0102\u00ADn"))
                .isEqualTo("Vset\u00EDn");
        assertThat(repairMojibake("Audi A4 Avant 2.0 TSI 110kW S-tronic 41tkm - z\u0102\u02C7ruka 2 roky"))
                .isEqualTo("Audi A4 Avant 2.0 TSI 110kW S-tronic 41tkm - z\u00E1ruka 2 roky");
        assertThat(repairMojibake("Audi A5 SPORTBACK FL.2023 2.0TDI 150KW 40TDI\u00E2\u20AC\u02D8S-TRONIC\u00E2\u20AC\u02D8Kamera"))
                .isEqualTo("Audi A5 SPORTBACK FL.2023 2.0TDI 150KW 40TDI\u2022S-TRONIC\u2022Kamera");
        assertThat(repairMojibake("BMW \u0139\u0098ada 3 GT 318D MANU\u0102\u0081L KAMERA V\u0102\u0165H\u0139\u0098EV"))
                .isEqualTo("BMW \u0158ada 3 GT 318D MANU\u00C1L KAMERA V\u00DDH\u0158EV");
        assertThat(repairMojibake("BMW 530D XDRIVE M SPORT 210KW M2021 K\u0139\u00AE\u0139\u02DDE TA\u0139\u02DDN\u0102\u2030 Z\u0102\u0081RUKA CZ DPH"))
                .isEqualTo("BMW 530D XDRIVE M SPORT 210KW M2021 K\u016E\u017DE TA\u017DN\u00C9 Z\u00C1RUKA CZ DPH");
        assertThat(repairMojibake("BMW 3 GT 320D 135kW xDrive LUXURY / VELMI P\u00C4\u0161KN\u0102\u0165 STAV VOZU/"))
                .isEqualTo("BMW 3 GT 320D 135kW xDrive LUXURY / VELMI P\u011AKN\u00DD STAV VOZU/");
        assertThat(repairMojibake("\u00C4\u015Aesk\u0102\u02C7 L\u0102\u00ADpa"))
                .isEqualTo("\u010Cesk\u00E1 L\u00EDpa");
        assertThat(repairMojibake("Praha - v\u0102\u00BDchod"))
                .isEqualTo("Praha - v\u00FDchod");
        assertThat(repairMojibake("Opel Astra 1.5D Sports Tourer Elegance 2024 - odpo\u00C4\u0164et DPH"))
                .isEqualTo("Opel Astra 1.5D Sports Tourer Elegance 2024 - odpo\u010Det DPH");
        assertThat(repairMojibake("Opel Vivaro 2.0 CDTI 84 kW (Long, 9 m\u0102\u00ADst) \u00E2\u20AC\u201C L2H1"))
                .isEqualTo("Opel Vivaro 2.0 CDTI 84 kW (Long, 9 m\u00EDst) \u2013 L2H1");
        assertThat(repairMojibake("\u00E2\u015B\u2026Peugeot 308 SW 1.2 81kw PureTech |2021| 69tkm |SERVIS|\u00C4\u015AR"))
                .isEqualTo("\u2705Peugeot 308 SW 1.2 81kw PureTech |2021| 69tkm |SERVIS|\u010CR");
        assertThat(repairMojibake("PEUGEOT 3008 2.0HDi 110KW GT-LINE 1.MAJITEL-PERF.STAV LED\u00E2\u00AD\u0090 -"))
                .isEqualTo("PEUGEOT 3008 2.0HDi 110KW GT-LINE 1.MAJITEL-PERF.STAV LED\u2B50 -");
        assertThat(repairMojibake("Renault Clio Grandtour, 2018, vyh\u0139\u2122.sed, navi ,DPH, Z\u0102\u0081RUKA"))
                .isEqualTo("Renault Clio Grandtour, 2018, vyh\u0159.sed, navi ,DPH, Z\u00C1RUKA");
        assertThat(repairMojibake("P\u0139\u2122edn\u0102\u00AD n\u0102\u02C7razn\u0102\u00ADk"))
                .isEqualTo("P\u0159edn\u00ED n\u00E1razn\u00EDk");
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

    private boolean looksLikelyFalseManual(String title, String transmission) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksLikelyFalseManual", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, transmission);
    }

    private boolean looksAutomaticHybridTitle(String title, String fuelType) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksAutomaticHybridTitle", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, fuelType);
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

    private String preferExplicitTitleFuelType(String title, String fuelType) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("preferExplicitTitleFuelType", String.class, String.class);
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

    private String repairMojibake(String value) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("repairMojibake", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
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

    private boolean looksBrandMismatch(String title, String url) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksBrandMismatch", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, url);
    }

    private boolean looksModelUrlMismatch(String title, String url) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksModelUrlMismatch", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, url);
    }

    private boolean looksBrokenOrForPartsListing(String title, String text) throws Exception {
        Method method = BazosParser.class.getDeclaredMethod("looksBrokenOrForPartsListing", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, title, text);
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
