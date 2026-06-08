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
        assertThat(resolveFuelType("ford fiesta st atmosfera 150ps", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("renault clio 1,2 54kw limited navigace", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("audi a6 c4 1.8 5v 160000km top stav", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("ford focus st 2.0 184kw", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("opel meriva 1.4 88 kw klima servis", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("peugeot 2008 e-2008 100kw 136hp", "")).isEqualTo("ELECTRIC");
        assertThat(resolveFuelType("bmw x7 xdrive40d", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("mercedes-benz glc 43 amg 4matic", "")).isEqualTo("PETROL");
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
        assertThat(resolveFuelType("dongfeng u-tour 1,5 t 130 kw exclusivefr 7mist", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("hyundai i30 1,5dpi style comfort plus", "")).isEqualTo("PETROL");
        assertThat(resolveFuelType("audi a4 avant 2.0td aut vyhrev senzory", "")).isEqualTo("DIESEL");
        assertThat(resolveTransmission("audi a4 avant 2.0td aut vyhrev senzory", "", "DIESEL")).isEqualTo("AUTOMATIC");
        assertThat(resolveFuelType("ssangyong korando 2.2td 4x4 manual", "")).isEqualTo("DIESEL");
        assertThat(resolveFuelType("skoda octavia iv 2.0 tdi dsg 4x4 150 ps", "")).isEqualTo("DIESEL");
        assertThat(resolveTransmission("honda crv 2020 hybrid benzin 72tis.km", "", "HYBRID")).isEqualTo("AUTOMATIC");
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
        assertThat(resolveCarType("toyota corolla verso automat 1,8i vvt-i", "")).isEqualTo("MINIVAN");
        assertThat(resolveCarType("mercedes-benz tridy m ml 320 3,0d v6", "")).isEqualTo("SUV");
        assertThat(resolveCarType("bmw rada 4 420d 2,0 gran kupe automat", "")).isEqualTo("SEDAN");
    }

    @Test
    void skipsNonCarSbazarListingsFromFreshLogs() throws Exception {
        assertThat(looksNonCarListing("padlo a rucni pumpicka")).isTrue();
        assertThat(looksNonCarListing("pc pocitac")).isTrue();
        assertThat(looksNonCarListing("osobni vuz https www sbazar cz inzerat 231248942 osobni vuz")).isTrue();
        assertThat(looksNonCarListing("osobni vuz opel corsa e 1.4 xel automat")).isFalse();
        assertThat(looksNonCarListing("auto https www sbazar cz inzerat 231274019 auto")).isTrue();
        assertThat(looksNonCarListing("nissan qashqai parkovaci senzor")).isTrue();
        assertThat(looksNonCarListing("nissan qashqai sklo zrcatka")).isTrue();
        assertThat(looksNonCarListing("suzuki vitara zadni sklo")).isTrue();
        assertThat(looksNonCarListing("triumph america lt cr 2015")).isTrue();
        assertThat(looksNonCarListing("bmw i3 125 kw 120 ah tep.cerpadlo")).isFalse();
        assertThat(looksCommercialVehicle("opel vivaro r.v. 2010 https www sbazar cz inzerat 231253178 opel vivaro rv 2010")).isTrue();
    }

    @Test
    void ignoresPageMetadataYearsAroundListingDates() throws Exception {
        assertThat(extractYear("mitsubishi outlander 2,2 di-d 110kw 4x4 -tk do6/27 vlozeno 2025")).isNull();
        assertThat(extractYear("peugeot 207 cc 1.6i r.v.2011 serviska stk 09/27 vlozeno 2025")).isEqualTo(2011);
        assertThat(resolveYear("opel insignia 2.0 aut kamera vyhrev serviska", "opel insignia vlozeno 2025"))
                .isNull();
        assertThat(resolveYear("opel insignia 2.0 aut kamera vyhrev serviska", "opel insignia rok 2017"))
                .isEqualTo(2017);
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

    @Test
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
