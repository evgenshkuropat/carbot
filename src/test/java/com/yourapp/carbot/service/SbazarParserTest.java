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
        assertThat(resolveFuelType("bmw rada 3 330e xdrive m-paket propano", "")).isEqualTo("PLUGIN_HYBRID");
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
        assertThat(resolveFuelType("jaecoo 7 1.6 phev shs exclusive", "")).isEqualTo("PLUGIN_HYBRID");
        assertThat(resolveFuelType("omoda 9 shs premium", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("hyundai inster cross premium", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("omoda 5 1.6 t 108 kw premium a/t", "", "PETROL")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("mazda cx-5 2.2 skyactive 4wd bose sendo", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("mazda cx-5 2.0 kangei 2wd 165 ps", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mazda cx-3 2.0 sky-g 121k attraction a/t", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("alfa romeo 147 gta 3,2busso manual", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("chevrolet matiz 0.8 rok 2009 krasny stav", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford s-max 1.5 st-line 118 kw 1 majitel", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford b-max 1.0 74kw colourline", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("citroen c4 1.2 e-thp 81kw shine", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("skoda fabia 1,2 htp classic", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("dacia jogger expression eco-g 120 5 mist", "")).isEqualTo("LPG");
        assertThat(resolveFuelType("tesla model 3 performance", "")).isEqualTo("ELECTRIC");
        assertThat(resolveFuelType("opel zafira a 2,0 74 kw nova stk", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("peugeot 3008 1.2 96kw nove rozvody", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("fiat 500x 1.6 81kw pop star navi", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes-benz cl 500", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("jaguar xk8 4.0 209 kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("nissan qashqai tekna e-power 2wd 140kw", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("bmw rada 2 225xe iperformance f45 165kw", "")).isEqualTo("PLUGIN_HYBRID");
        assertThat(resolveFuelType("top motor 4x4 1.8 t 110 kw skoda octavia super", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("nova stk bez koroze chevrolet cruze 1.6 91kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford focus combi 1.6 85 kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford focus 1.6 combi", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford fiesta st atmosfera 150ps", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("renault clio 1,2 54kw limited navigace", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("audi a6 c4 1.8 5v 160000km top stav", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("audi a8 d3 long 4.2 mpi 246kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford focus st 2.0 184kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("opel meriva 1.4 88 kw klima servis", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("peugeot 2008 e-2008 100kw 136hp", "")).isEqualTo("ELECTRIC");
        assertThat(resolveFuelType("bmw x7 xdrive40d", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("mercedes-benz glc 43 amg 4matic", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes benz glc 300e", "")).isEqualTo("PLUGIN_HYBRID");
        assertThat(resolveTransmission("mercedes benz glc 300e", "", "PLUGIN_HYBRID")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("volvo xc60 2.0b4 4x4 145kw cr dph core", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("toyota gr86 executive manualni prev odpocet dph", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("vw golf 8 variant 1.5 etsi 110kw dsg", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("skoda octavia 1.5tgi 96kw dsg ambition 9/20", "")).isEqualTo("CNG");
        assertThat(resolveFuelType("mercedes-benz tridy b b-class 250 edrive 132kw", "")).isEqualTo("ELECTRIC");
        assertThat(resolveFuelType("suzuki sx4 s-cross 1,4 boosterjet premium 2x4", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("skoda superb 1.4 tsi 160kw iv sportline dsg", "")).isEqualTo("PLUGIN_HYBRID");
        assertThat(resolveFuelType("mercedes-benz tridy c e performance 4m amg f1 edit", "")).isEqualTo("PLUGIN_HYBRID");
        assertThat(resolveFuelType("volkswagen passat 2.0 tsi elegance r line dsg", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("peugeot 2008 2017 1.2 81kw automat 70 000 km", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("suzuki grand vitara 2.4 4x4 uzaverka", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("suzuki grand vitara 4x4 2,0 103 kw tazne", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("suzuki swift 1.2 violet edition 2015", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("nissan patrol 4x4 3,0 di turbo 116kw", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("infiniti fx37 s 4x4 3,7 v6 235kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes e240 w211", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes benz clk 200 kompressor automat", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mercedes-benz tridy c c200 1.6 automat 46000 km", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("volkswagen golf 3 1.4 cl 44 kw 1993 oldtimer", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("dacia duster 1,6", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("dacia logan mcv 1.0sce puvod cr odpdph manualni", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("suzuki samurai 1.3 4x4 1994 celoplech", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("saab 9-3 1,9 tid 110kw automat 2xkola", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("ostatni ktm x-bow gt-xr 2024 nove", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("dongfeng u-tour 1,5 t 130 kw exclusivefr 7mist", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("hyundai i30 1,5dpi style comfort plus", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("audi a4 avant 2.0td aut vyhrev senzory", "")).isEqualTo("DIESEL");
        assertThat(resolveTransmission("audi a4 avant 2.0td aut vyhrev senzory", "", "DIESEL")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("ssangyong korando 2.2td 4x4 manual", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("jeep cherokee 2.2 147kw 4x4 limited kuze nav", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("jeep compass 1.3 110kw limited kamera navi", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("toyota yaris 1.0 vvt-i 51kw cr", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("prodam toyota yaris 1.33 vvt-i 73kw automat", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("volkswagen e-up 60 kw serviska top stav", "")).isEqualTo("ELECTRIC");
        assertThat(resolveFuelType("bmw i4 edrive 40 120 000 km tazne keyless soh 94", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("bmw i4 edrive 40 120 000 km tazne keyless soh 94", "", "ELECTRIC")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("bmw ix1 xdrive30 m zaruka tazne cr", "")).isEqualTo("ELECTRIC");
        assertThat(resolveTransmission("bmw ix1 xdrive30 m zaruka tazne cr", "", "ELECTRIC")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("toyota corolla 1,8 hsd 122ps ts gr sport a/t", "")).isEqualTo("HYBRID");
        assertThat(resolveFuelType("ford focus 2,0 eb 184kw st navi", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mazda mx-5 1.5 96 kw softtop", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("toyota rav4 2,0 158ps life a/t 4x4", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mitsubishi outlander 2,0 150ps intense a/t 4x4", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("mitsubishi asx 2,0 150ps inform", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("seat mii 1.0 44kw 5dveri", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("porsche cayman gt4 wrap od koenigsegg znama historie", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("skoda octavia iv 2.0 tdi dsg 4x4 150 ps", "")).isEqualTo("DIESEL");
        assertThat(resolveTransmission("honda crv 2020 hybrid benzin 72tis.km", "", "HYBRID")).isEqualTo("AUTOMATIC");
        assertThat(resolveTransmission("toyota camry 2.5 hybrid executive 169kw", "", "HYBRID")).isEqualTo("AUTOMATIC");
        assertThat(resolveTransmission("seat leon, 1,4 tsi e-hybrid fr line led", "", "HYBRID")).isEqualTo("AUTOMATIC");
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
        assertThat(detectBrand("dongfeng t5 evo 1,5 t 130kw dct7 dragonedition")).isEqualTo("DONGFENG");
        assertThat(detectBrand("isuzu d-max v-cross 2.2l 163k 4x4 8st a/t")).isEqualTo("ISUZU");
        assertThat(detectBrand("opel mokka x 1,6 16v")).isEqualTo("OPEL");
        assertThat(detectBrand("subaru xv 2.0i 4x4 aut")).isEqualTo("SUBARU");
        assertThat(detectBrand("saab 9-3 1,9 tid 110kw automat 2xkola")).isEqualTo("SAAB");
        assertThat(detectBrand("ostatni ktm x-bow gt-xr 2024 nove")).isEqualTo("KTM");
        assertThat(detectBrand("opel tigra roadster 1.4 16v benzin")).isEqualTo("OPEL");
        assertThat(detectBrand("hyudai i10 1.0 49 kw hatchback")).isEqualTo("HYUNDAI");
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
        assertThat(resolveCarType("volkswagen taigo 1,5 tsi dsg r-line led assist", "")).isEqualTo("SUV");
        assertThat(resolveCarType("vw golf plus 2.0tdi 103 kw dsg", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("ford fusion 1.4i 16v klima tazne alu manualni", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("suzuki samurai 1.3 4x4 1994 celoplech", "")).isEqualTo("SUV");
        assertThat(resolveCarType("fiat sedici 1.9jtd 4x4 klimatizace", "")).isEqualTo("SUV");
        assertThat(resolveCarType("citroen c4 2022 nafta automat dph", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("opel grandland 2021 automat dph", "")).isEqualTo("SUV");
        assertThat(resolveCarType("honda insight 1.3i hybrid automat klima alu", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("ford puma gen-e premium 123kw 43 kwh", "")).isEqualTo("SUV");
        assertThat(resolveCarType("toyota avensis com 1.8i 95kw r.2005", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("hyundai i10 1.1i rv 2011", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("suzuki splash 1.0i 48kw r.5/2011", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("mitsubishi eclipse cross 1,5 t-gdi 120kw cr", "")).isEqualTo("SUV");
        assertThat(resolveCarType("toyota proace city 1.5 d-4d 100 active swb cr", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("toyota verso 1,8i comfort benzin 108kw", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("ostatni ktm x-bow gt-xr 2024 nove", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("saab 9-3 1,9 tid 110kw automat 2xkola", "")).isEqualTo("SEDAN");
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
        assertThat(resolveCarType("ford focus turnier 1,6i 85kw benzin fun-x", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("nissan padfinder", "")).isEqualTo("SUV");
        assertThat(resolveCarType("audi a4 avant 1,4 tfsi aut led navi e kufr", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("mercedes benz a 45 amg", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("hyundai bayon bayon fl 1,0 t-gdi wave", "")).isEqualTo("SUV");
        assertThat(resolveCarType("hyundai inster cross premium", "")).isEqualTo("SUV");
        assertThat(resolveCarType("jaecoo 7 1.6 phev shs exclusive", "")).isEqualTo("SUV");
        assertThat(resolveCarType("omoda 9 shs premium", "")).isEqualTo("SUV");
        assertThat(resolveCarType("peugeot 3008 allure hybrid extra-stav nove", "")).isEqualTo("SUV");
        assertThat(resolveCarType("volkswagen troc 1.5tsi 110kw 2022", "")).isEqualTo("SUV");
        assertThat(resolveCarType("peugeot 2008 e-2008 100kw 136hp", "")).isEqualTo("SUV");
        assertThat(resolveCarType("renault captur evolution tce 115 my25", "")).isEqualTo("SUV");
        assertThat(resolveCarType("hyundai ix35 2,0 crdi serviska", "")).isEqualTo("SUV");
        assertThat(resolveCarType("ford edge 2,0 ecoblue 175kw st-line", "")).isEqualTo("SUV");
        assertThat(resolveCarType("volkswagen id.4", "")).isEqualTo("SUV");
        assertThat(resolveCarType("volkswagen id.4 150kw dcc max 1st plus pro pf", "")).isEqualTo("SUV");
        assertThat(resolveCarType("tesla model y rwd lfp", "")).isEqualTo("SUV");
        assertThat(resolveCarType("jeep renegade 1.4 multiair 103kw limited", "")).isEqualTo("SUV");
        assertThat(resolveCarType("audi tt 2.0 tfsi s-line top", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("bmw e46 325ti compact automat", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("mercedes-benz eqe 300 hyperscreen tazne soh98,8", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes 211", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("suzuki jimny 1.3 60kw 4x4 tazne", "")).isEqualTo("SUV");
        assertThat(resolveCarType("ford mustang convertible 5.0ti-vct v8 gt automat", "")).isEqualTo("CABRIO");
        assertThat(resolveCarType("jaguar f-type s 3.0 380 ps kamera panorama", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("bmw m2 competition cr hk", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("suzuki grand vitara 2,4i", "")).isEqualTo("SUV");
        assertThat(resolveCarType("hyundai i30 cw 1.6d r.v. 2010", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("ford tourneo connect 1.6 tdci 70kw nove rozvody", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("volkswagen golf sportsvan 1.4tsi 92kw jen 114 tkm vybava", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("renault trafic 1,6 dci nafta manualni", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("subaru outback 2.5 lpg r.v. 2006", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("volvo v50 1.6d 80kw kuze vyhrev handsfre", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("volvo s40 1.6d 80kw vyhrev sed nova stk", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("citroen c-crosser 2.2 hdi 4x4", "")).isEqualTo("SUV");
        assertThat(resolveCarType("kia stonic 1.2 i nove v cr 1 maj", "")).isEqualTo("SUV");
        assertThat(resolveCarType("peugeot partner 1,5 bhdi 75kw", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("prodam dacia logan mcv 1.2 55 kw 2014", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("kia proceed gt 1.6 t-gdi 150 kw r.v. 2023", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("mercedes-benz cls 1 majitel amg full led 4 matic", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mazda 6 wagon 2.2d skyactiv awd pouze 66 tiskm cr", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("skoda octavia 2.0tdi 110kw 1 majitel cr vyhr", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("hyundai bayon bayon fl 1,0 t-gdi wave", "combi wagon")).isEqualTo("SUV");
        assertThat(resolveCarType("skoda roomster 1.2tsi 63kw nove v cr klima", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("seat leon, 1,4 tsi e-hybrid fr line led", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("seat leon, st fr 1.4tsi92kw 1maj facelift", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("skoda forman confortline", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("audi a6 3.0 tdi 4x4 avant quattro automat sline", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("fiat bravo 2011 1.4 66kw lpg servis nove rozvody", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("chevrolet matiz 0.8 rok 2009 krasny stav", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("mercedes w 220 320 i", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("tesla model 3 performance", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("ford b-max 1.0 74kw colourline", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("volkswagen t5 2.0tdi 9mist", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("volkswagen t6 2.0 tdi 84 kw", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("dacia jogger expression eco-g 120 5 mist", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("ssangyong tivoli xlv 1.6i 94kw lpg", "")).isEqualTo("SUV");
        assertThat(resolveCarType("renault fluence 1.5 dci 81 kw klimatizace", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("audi s8 tfsi quattro full bo masaze", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("jaguar xf 2.0 25d 177kw", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("renault koleos 4x4 2.0dci 110kw", "")).isEqualTo("SUV");
        assertThat(resolveCarType("fiat 500x 1.6 81kw pop star navi", "")).isEqualTo("SUV");
        assertThat(resolveCarType("land rover freelander 2.0td4 82kw", "")).isEqualTo("SUV");
        assertThat(resolveCarType("land rover defender 2.0d 110 awd cr tazne 360", "")).isEqualTo("SUV");
        assertThat(resolveCarType("dacia bigster journey hybrid 155", "")).isEqualTo("SUV");
        assertThat(resolveCarType("mercedes-benz cl 500", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("jaguar xk8 4.0 209 kw", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("dodge caliber", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("dodge ram hemi lpg offroad paket", "")).isEqualTo("PICKUP");
        assertThat(resolveCarType("skoda fabia 1.2 htp classic", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("ford tourneo custom 2,0 ecoblue 96kw 8mist l2 navi", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("dongfeng u-tour 1,5 t 130 kw exclusivefr 7mist", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("dongfeng mage 1,5 t 145 kw e2 dct7", "")).isEqualTo("SUV");
        assertThat(resolveCarType("dongfeng t5 evo 1,5 t 130kw dct7 dragonedition", "")).isEqualTo("SUV");
        assertThat(resolveCarType("bmw x2 sdrive 18d m-paket", "")).isEqualTo("SUV");
        assertThat(resolveCarType("seat arona 1,6 tdi 85kw fr", "")).isEqualTo("SUV");
        assertThat(resolveCarType("klima bez koroze tempomat seat toledo 1.9 tdi 81kw", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("alfa romeo stelvio 2.2 jtdm competizione q4", "")).isEqualTo("SUV");
        assertThat(resolveCarType("suzuki sx4 s-cross 1,4 boosterjet premium 2x4", "")).isEqualTo("SUV");
        assertThat(resolveCarType("peugeot 308 2,0 bhdi 110kw", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("peugeot 308 sw 1,2 pt eat8 pripravujeme", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("peugeot 308 1,6 hdi automat", "coupe cabrio")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("opel insignia 2.0 aut kamera vyhrev serviska", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("opel insignia 2.0cdti manual vyhrev tazne", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("skoda octavia scout cz dph", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("bmw rada 2 225xe iperformance f45 165kw", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("toyota sienna awd 2017 7 mist 8at tazne", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("toyota starlet", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("toyota auris 1.4 d-4d 66kw koupeno cr 2016", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("toyota land cruiser 2,8 mhev invincible", "")).isEqualTo("SUV");
        assertThat(resolveCarType("toyota urban cruiser 1,3i 74 kw spojka stk", "")).isEqualTo("SUV");
        assertThat(resolveCarType("nissan terrano ii 2.7td 92kw puvod cr nova stk", "")).isEqualTo("SUV");
        assertThat(resolveCarType("citroen jumpy 2.0hdi 94kw 8mist", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("renault kangoo 1.6 cng", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("mitsubishi colt 1,3 70kw klima", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("peugeot 207 cc 1.6i r.v.2011 serviska stk", "")).isEqualTo("CABRIO");
        assertThat(resolveCarType("opel agila 1.2 16v gl", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("opel mokka 1.6 tdci 4x4 innovation xenon navigace", "")).isEqualTo("SUV");
        assertThat(resolveCarType("opel cascada 2.0cdti 121kw 165k", "")).isEqualTo("CABRIO");
        assertThat(resolveCarType("audi a7 3.0 tdi 180 kw quattro", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("vw jetta highline 1.4tsi", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("kia ev6 gt 430kw 4x4 77kwh zaruka", "")).isEqualTo("SUV");
        assertThat(resolveCarType("bmw i5 i5 xdrive40 led tazne", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes-benz citan 1.5dci mixto dlouhe", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("kia k4 hb 1,6 t-gdi gpf 7dct top", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("mercedes w447 v250 avantgarde", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("mercedes v 2.2 cdi at 7mist 4x4 dph serviska", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("land rover defender 110 hcpu", "")).isEqualTo("PICKUP");
        assertThat(resolveCarType("citroen c5 1.6hdi r.v.2010", "cabrio coupe")).isEqualTo("SEDAN");
        assertThat(resolveCarType("kia magentis 2.0crdi top stav", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mazda 6 2.0 121kw skyactiv automat", "minivan mpv")).isEqualTo("SEDAN");
        assertThat(resolveCarType("ssangyong korando 2.2td 4x4 manual", "")).isEqualTo("SUV");
        assertThat(resolveCarType("subaru xv 2.0i 4x4 aut", "")).isEqualTo("SUV");
        assertThat(resolveCarType("dacia logan 1,2 16v klima navi temp", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes e240 w211", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mitsubishi outlender", "")).isEqualTo("SUV");
        assertThat(resolveCarType("infiniti fx37 s 4x4 3,7 v6 235kw", "")).isEqualTo("SUV");
        assertThat(resolveCarType("lexus gx 460 4,6 v8", "")).isEqualTo("SUV");
        assertThat(resolveCarType("volkswagen passat highline 2,0 tdi 103kw dsg kola", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("bmw x3 xdrive30i koupeno v cr serviska 1.majitel", "")).isEqualTo("SUV");
        assertThat(resolveCarType("bentley continental gt v12 434 kw breitling masaze", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("porsche cayman gt4 wrap od koenigsegg znama historie", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("volkswagen california 2.0 tdi dsg abt xnh camper", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("audi s6 55tdi 257kw virtual led kuze záruka", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("audi s7 sportback 3.0 tfsi", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("renault modus 1,2i 55kw stk do 6/2028", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("hyundai kona 1,0 t-gdi 88 kw comfort", "")).isEqualTo("SUV");
        assertThat(resolveCarType("toyota corolla verso automat 1,8i vvt-i", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("mercedes-benz tridy m ml 320 3,0d v6", "")).isEqualTo("SUV");
        assertThat(resolveCarType("bmw rada 4 420d 2,0 gran kupe automat", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("peugeot 301 1.2 puretech 2017", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("peugeot 508gt plug in hybrid 165kw e-eat8", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes-benz cle 200 amg line", "")).isEqualTo("COUPE");
        assertThat(resolveCarType("citroen c2 1.1 i", "")).isEqualTo("HATCHBACK");
        assertThat(resolveCarType("bmw ix1 xdrive30 m zaruka tazne cr", "")).isEqualTo("SUV");
        assertThat(resolveCarType("mercedes-benz gle 300d 4m cr 1 maj dph acc 360", "")).isEqualTo("SUV");
        assertThat(resolveCarType("mazda mx-5 1.5 96 kw softtop 48k km", "")).isEqualTo("CABRIO");
        assertThat(resolveCarType("alfa romeo 159 2,4 jtdm 154 kw zachovaly stav", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("toyota corolla ts 1,2 turbo 115ps comfort", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("toyota corolla 1,8 hsd 122ps ts gr sport a/t", "")).isEqualTo("WAGON");
        assertThat(resolveCarType("toyota avensis 2.2 d-4d 110 kw plny servis", "")).isEqualTo("SEDAN");
        assertThat(resolveCarType("mercedes benz ml 400cdi w163 facelift automat kuze", "")).isEqualTo("SUV");
    }

    @Test
    void skipsNonCarSbazarListingsFromFreshLogs() throws Exception {
        assertThat(looksNonCarListing("padlo a rucni pumpicka")).isTrue();
        assertThat(looksNonCarListing("pc pocitac")).isTrue();
        assertThat(looksNonCarListing("osobni vuz https www sbazar cz inzerat 231248942 osobni vuz")).isTrue();
        assertThat(looksNonCarListing("osobni vuz opel corsa e 1.4 xel automat")).isFalse();
        assertThat(looksNonCarListing("auto https www sbazar cz inzerat 231274019 auto")).isTrue();
        assertThat(looksNonCarListing("prodam https www sbazar cz inzerat 232430412 prodam")).isTrue();
        assertThat(looksNonCarListing("prodam bmw i4 edrive 40")).isFalse();
        assertThat(looksNonCarListing("nissan qashqai parkovaci senzor")).isTrue();
        assertThat(looksNonCarListing("nissan qashqai sklo zrcatka")).isTrue();
        assertThat(looksNonCarListing("suzuki vitara zadni sklo")).isTrue();
        assertThat(looksNonCarListing("5g0827469a zadni kamera")).isTrue();
        assertThat(looksNonCarListing("triumph america lt cr 2015")).isTrue();
        assertThat(looksNonCarListing("bmw i3 125 kw 120 ah tep.cerpadlo")).isFalse();
        assertThat(looksNonCarListing("posilovac krouticiho momentu")).isTrue();
        assertThat(looksNonCarListing("toyota yaris 1.3benzin klima 5l/100km z+l pneu")).isFalse();
        assertThat(looksNonCarListing("jeep grand cherokee wj packy blinkr a paska")).isTrue();
        assertThat(looksCommercialVehicle("opel vivaro r.v. 2010 https www sbazar cz inzerat 231253178 opel vivaro rv 2010")).isTrue();
        assertThat(looksCommercialVehicle("peugeot expert 2.0 hdi")).isTrue();
    }

    @Test
    void ignoresPageMetadataYearsAroundListingDates() throws Exception {
        assertThat(extractYear("mitsubishi outlander 2,2 di-d 110kw 4x4 -tk do6/27 vlozeno 2025")).isNull();
        assertThat(extractYear("peugeot 207 cc 1.6i r.v.2011 serviska stk 09/27 vlozeno 2025")).isEqualTo(2011);
        assertThat(resolveYear("opel insignia 2.0 aut kamera vyhrev serviska", "opel insignia vlozeno 2025"))
                .isNull();
        assertThat(resolveYear("opel insignia 2.0 aut kamera vyhrev serviska", "opel insignia rok 2017"))
                .isEqualTo(2017);
        assertThat(resolveYear("seat leon 2007 2.0 tdi dsg", "seat leon https www sbazar cz inzerat 231659934"))
                .isEqualTo(2007);
        assertThat(resolveYear("skoda yeti 1.2 tsi dsg 2011", "skoda yeti https www.sbazar.cz inzerat 228718145"))
                .isEqualTo(2011);
    }

    @Test
    void resolvesFreshSbazarLogSignals() throws Exception {
        assertThat(resolveFuelType("mitsubishi outlander 2.2 di-d mivec instyle7 4x4", ""))
                .isEqualTo("DIESEL");
        assertThat(resolveCarType("renault megane 2 2006", ""))
                .isEqualTo("HATCHBACK");
        assertThat(resolveCarType("ssangyong rexton 2.2td 4x4 aut kam 7mist tazne", ""))
                .isEqualTo("SUV");
        assertThat(resolveCarType("mercedes-benz gl420 cdi 2007", ""))
                .isEqualTo("SUV");
        assertThat(resolveCarType("jaguar xe r-sport 20d awd 132kw", ""))
                .isEqualTo("SEDAN");
        assertThat(resolveCarType("pajero pinin 3.2 did tazne", ""))
                .isEqualTo("SUV");
        assertThat(extractMileage("renault clio rok vyroby 2008 km"))
                .isNull();
    }

    @Test
    void readsMileageFromTitleBeforeNoisyPageText() throws Exception {
        String identity = "opel astra 1.6 85 kw 101 tis. km";
        String noisyScopedText = identity + " 6022 dalsi metadata";

        assertThat(firstInteger(extractMileage(identity), extractMileage(noisyScopedText))).isEqualTo(101_000);
    }

    void repairsSbazarMojibakeBeforeOutput() throws Exception {
        assertThat(repairMojibake("v okres Uhersk\u0102\u00A9 Hradi\u0139\u02C7t\u00C4\u203A"))
                .isEqualTo("v okres Uherské Hradiště");
        assertThat(repairMojibake("\u0139\u00A0koda Octavia Combi 1.9 TDI 96kw ASZ"))
                .isEqualTo("Škoda Octavia Combi 1.9 TDI 96kw ASZ");
        assertThat(repairMojibake("Nov\u0102\u02C7 STK, \u00C4\u015Aesk\u0102\u02C7 L\u0102\u00ADpa"))
                .isEqualTo("Nová STK, Česká Lípa");
        assertThat(repairMojibake("BOHAT\u0102\u0081 V\u0102\u0165BAVA"))
                .isEqualTo("BOHATÁ VÝBAVA");
    }

    @Test
    void repairsFreshSbazarMojibakeAndSignalsFromLogs() throws Exception {
        assertThat(repairMojibake("v okres Uhersk\u0102\u00A9 Hradi\u0139\u02C7t\u00C4\u203A"))
                .isEqualTo("v okres Uhersk\u00E9 Hradi\u0161t\u011B");
        assertThat(repairMojibake("\u0139\u00A0koda Octavia Combi 1.9 TDI 96kw ASZ"))
                .isEqualTo("\u0160koda Octavia Combi 1.9 TDI 96kw ASZ");
        assertThat(repairMojibake("Ford Fusion 1.4i, 59 kW, r.2009, nov\u0102\u02C7 STK"))
                .isEqualTo("Ford Fusion 1.4i, 59 kW, r.2009, nov\u00E1 STK");
        assertThat(repairMojibake("v Ho\u0139\u2122ovice"))
                .isEqualTo("v Ho\u0159ovice");
        assertThat(repairMojibake("Mercedes-Benz T\u0139\u2122\u0102\u00ADdy C, 180 CDI"))
                .isEqualTo("Mercedes-Benz T\u0159\u00EDdy C, 180 CDI");
        assertThat(repairMojibake("v okres Hlavn\u0102\u00AD m\u00C4\u203Asto Praha"))
                .isEqualTo("v okres Hlavn\u00ED m\u011Bsto Praha");
        assertThat(repairMojibake("Ta\u0139\u013En\u0102\u00A9 Mas\u0102\u02C7"))
                .isEqualTo("Ta\u017En\u00E9 Mas\u00E1");
        assertThat(repairMojibake("v Krom\u00C4\u203A\u0139\u2122\u0102\u00AD\u0139\u013E"))
                .isEqualTo("v Krom\u011B\u0159\u00ED\u017E");
        assertThat(repairMojibake("v Hodon\u0102\u00ADn"))
                .isEqualTo("v Hodon\u00EDn");
        assertThat(repairMojibake("v Old\u0139\u2122i\u0139\u02C7"))
                .isEqualTo("v Old\u0159i\u0161");
        assertThat(repairMojibake("v \u00C4\u015Ael\u0102\u02C7kovice"))
                .isEqualTo("v \u010Cel\u00E1kovice");
        assertThat(repairMojibake("v P\u0102\u00ADsek"))
                .isEqualTo("v P\u00EDsek");

        assertThat(resolveCarType("fiat ulysse ulysse2.2 mtj 180k 8at l2", ""))
                .isEqualTo("MINIVAN");
        assertThat(resolveCarType("fiat doblo panorama", ""))
                .isEqualTo("MINIVAN");
        assertThat(resolveCarType("ford tourneo courier 2021", ""))
                .isEqualTo("MINIVAN");
        assertThat(resolveCarType("chrysler town country 3,6 rt penta dvd 2014", ""))
                .isEqualTo("MINIVAN");
        assertThat(resolveCarType("toyota camry executive", ""))
                .isEqualTo("SEDAN");
        assertThat(detectBrand("opel crossland x 1.2t automat nove rozvody"))
                .isEqualTo("OPEL");
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

    private boolean looksCommercialVehicle(String searchable) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("looksCommercialVehicle", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(parser, searchable);
    }

    private Integer extractYear(String searchable) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("extractYear", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, searchable);
    }

    private Integer resolveYear(String identityText, String scopedText) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("resolveYear", String.class, String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, identityText, scopedText);
    }

    private Integer extractMileage(String searchable) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("extractMileage", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, searchable);
    }

    private String repairMojibake(String value) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("repairMojibake", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
    }

    private Integer firstInteger(Integer... values) throws Exception {
        Method method = SbazarParser.class.getDeclaredMethod("firstInteger", Integer[].class);
        method.setAccessible(true);
        return (Integer) method.invoke(parser, new Object[]{values});
    }
}
