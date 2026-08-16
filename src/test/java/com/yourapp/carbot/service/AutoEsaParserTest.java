package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AutoEsaParserTest {

    private final AutoEsaParser parser = new AutoEsaParser();

    @Test
    void parsesAutoEsaListCard() throws Exception {
        Document doc = Jsoup.parse("""
                <html>
                <body>
                <a href="/dacia/duster/suv/benzin/784455670" class="car_item" target="_blank">
                    <div class="car_item__image" style="background-image: url(/files/cars/784455670/950_713_e/784455670-1.jpg);">
                        <img src="/files/cars/784455670/950_713_e/784455670-1.jpg?1782401530" alt="Dacia Duster 1.3TCe 4x4">
                    </div>
                    <div class="car_item__content">
                        <h2 class="car_item__title" data-mh="title">Dacia Duster <span>2022</span></h2>
                        <div class="car_item__icons" data-mh="icons">
                            <div class="car_item__icon icon_year">1.3TCe</div>
                            <div class="car_item__icon icon_power">110 kW</div>
                            <div class="car_item__icon icon_4x4">4x4</div>
                            <div class="car_item__icon icon_fuel">benzin</div>
                            <div class="car_item__icon icon_range">56 718 km</div>
                        </div>
                        <div class="car_item__price_holder">
                            <div class="car_item__price_block">
                                <span class="text">Mesicne od</span>
                                <span class="price">1&nbsp;254&nbsp;Kc</span>
                            </div>
                            <div class="car_item__price_block text-red">
                                <span class="text">Akcni cena</span>
                                <span class="price">390&nbsp;000&nbsp;Kc</span>
                            </div>
                        </div>
                    </div>
                </a>
                </body>
                </html>
                """, "https://www.autoesa.cz/vsechna-auta");

        Element card = doc.selectFirst("a.car_item");
        CarDto car = parseCard(card, "https://www.autoesa.cz/dacia/duster/suv/benzin/784455670");

        assertThat(car.getSource()).isEqualTo("AUTOESA");
        assertThat(car.getTitle()).isEqualTo("Dacia Duster 1.3TCe 4x4");
        assertThat(car.getPriceValue()).isEqualTo(390_000);
        assertThat(car.getBrand()).isEqualTo("DACIA");
        assertThat(car.getYear()).isEqualTo(2022);
        assertThat(car.getMileage()).isEqualTo(56_718);
        assertThat(car.getFuelType()).isEqualTo("PETROL");
        assertThat(car.getCarType()).isEqualTo("SUV");
        assertThat(car.getUrl()).isEqualTo("https://www.autoesa.cz/dacia/duster/suv/benzin/784455670");
        assertThat(car.getImageUrl()).isEqualTo("https://www.autoesa.cz/files/cars/784455670/950_713_e/784455670-1.jpg");
    }

    @Test
    void parsesAutoEsaDetailAttributes() throws Exception {
        Document doc = Jsoup.parse("""
                <html>
                <body>
                <div class="detail_attr_inner">
                    <ul>
                        <li><strong>Rok</strong><span>2022</span></li>
                        <li><strong>Palivo</strong><span>benzin</span></li>
                        <li><strong>Prevodovka</strong><span>manual/6</span></li>
                        <li><strong>Karoserie</strong><span>SUV</span></li>
                        <li><strong>Stav tachometru</strong><span>56718 km</span></li>
                    </ul>
                </div>
                </body>
                </html>
                """);

        assertThat(extractDetailValue(doc, "Prevodovka")).isEqualTo("manual/6");
        assertThat(extractDetailValue(doc, "Karoserie")).isEqualTo("SUV");
        assertThat(extractDetailValue(doc, "Stav tachometru")).isEqualTo("56718 km");
    }

    @Test
    void repairsAutoEsaMojibakeTitles() throws Exception {
        assertThat(repairMojibake("\u0139\u00A0koda Octavia IV 2.0 TDi Style 4x4"))
                .isEqualTo("\u0160koda Octavia IV 2.0 TDi Style 4x4");
        assertThat(repairMojibake("Citro\u00C4\u201A\u00C2\u00ABn C3 Picasso 1.6i"))
                .isEqualTo("Citro\u00EBn C3 Picasso 1.6i");
        assertThat(repairMojibake("Renault Sc\u00C4\u201A\u00C2\u00A9nic 1.8 dCi Business"))
                .isEqualTo("Renault Sc\u00E9nic 1.8 dCi Business");
        assertThat(repairMojibake("Mercedes-Benz T\u0139\u2122\u0102\u00ADda C 1.6 d C 200 d"))
                .isEqualTo("Mercedes-Benz T\u0159\u00EDda C 1.6 d C 200 d");
        assertThat(repairMojibake("Citro\u0102\u00ABn C5 Aircross 1.5 HDI"))
                .isEqualTo("Citro\u00EBn C5 Aircross 1.5 HDI");
        assertThat(repairMojibake("\u0139\u00A0koda Superb III 2.0 TSI L&K 4x4"))
                .isEqualTo("\u0160koda Superb III 2.0 TSI L&K 4x4");
    }

    @Test
    void detectsBrandsSeenInAutoEsaLogs() throws Exception {
        assertThat(normalizeBrand("ds", "DS DS3 1.2PT")).isEqualTo("DS");
        assertThat(normalizeBrand("mg", "MG ZS 1.0 T-GDi Elegance")).isEqualTo("MG");
        assertThat(normalizeBrand("ssangyong", "SsangYong Korando 1.5T-GDi 4x4")).isEqualTo("SSANGYONG");
        assertThat(normalizeBrand("maserati", "Maserati GranTurismo 4.7 V8 S Automatic")).isEqualTo("MASERATI");
        assertThat(normalizeBrand("jaguar", "Jaguar F-Type 3.0 V6 V6 S AWD Coupe")).isEqualTo("JAGUAR");
    }

    private CarDto parseCard(Element card, String url) throws Exception {
        Method method = AutoEsaParser.class.getDeclaredMethod("parseCard", Element.class, String.class);
        method.setAccessible(true);
        return (CarDto) method.invoke(parser, card, url);
    }

    private String extractDetailValue(Document doc, String label) throws Exception {
        Method method = AutoEsaParser.class.getDeclaredMethod("extractDetailValue", Document.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, doc, label);
    }

    private String repairMojibake(String value) throws Exception {
        Method method = AutoEsaParser.class.getDeclaredMethod("repairMojibake", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, value);
    }

    private String normalizeBrand(String raw, String title) throws Exception {
        Method method = AutoEsaParser.class.getDeclaredMethod("normalizeBrand", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, raw, title);
    }
}
