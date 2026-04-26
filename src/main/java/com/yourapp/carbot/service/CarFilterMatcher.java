package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Year;
import java.util.Locale;

@Component
public class CarFilterMatcher {

    private static final int CURRENT_YEAR = Year.now().getValue();

    public boolean matches(CarEntity car, UserFilterEntity filter) {
        return check(car, filter).result();
    }

    public FilterCheckResult check(CarEntity car, UserFilterEntity filter) {
        if (car == null) {
            return new FilterCheckResult(false, false, false, false, false, false, false, false, false);
        }

        if (filter == null) {
            return new FilterCheckResult(true, true, true, true, true, true, true, true, true);
        }

        boolean carTypeOk = matchesCarType(car, filter);
        boolean brandOk = matchesBrand(car, filter);
        boolean maxPriceOk = matchesMaxPrice(car, filter);
        boolean locationOk = matchesLocation(car, filter);
        boolean mileageOk = matchesMaxMileage(car, filter);
        boolean fuelTypeOk = matchesFuelType(car, filter);
        boolean transmissionOk = matchesTransmission(car, filter);
        boolean yearOk = matchesYearFrom(car, filter);

        boolean result = carTypeOk
                && brandOk
                && maxPriceOk
                && locationOk
                && mileageOk
                && fuelTypeOk
                && transmissionOk
                && yearOk;

        return new FilterCheckResult(
                result,
                carTypeOk,
                brandOk,
                maxPriceOk,
                locationOk,
                mileageOk,
                fuelTypeOk,
                transmissionOk,
                yearOk
        );
    }

    private boolean matchesCarType(CarEntity car, UserFilterEntity filter) {
        String filterTypes = filter.getCarType();

        if (isBlank(filterTypes) || "ANY".equalsIgnoreCase(filterTypes.trim())) {
            return true;
        }

        String storedType = normalizeToken(car.getCarType());
        String title = " " + normalizeText(car.getTitle()) + " ";

        for (String rawType : filterTypes.split(",")) {
            String wanted = normalizeToken(rawType);

            if (wanted.isBlank()) {
                continue;
            }

            if (!storedType.isBlank() && storedType.equals(wanted)) {
                return true;
            }

            boolean matchedByTitle = switch (wanted) {
                case "SEDAN" -> containsAny(title,
                        " SEDAN ",
                        " LIMOUSINE ",
                        " LIMUZINA ",
                        " LIFTBACK ",
                        " FASTBACK ",
                        " SALOON ",
                        " OCTAVIA ",
                        " SUPERB ",
                        " PASSAT ",
                        " ARTEON ",
                        " A4 ",
                        " A6 ",
                        " A8 ",
                        " E90 ",
                        " E60 ",
                        " E39 ",
                        " MODEL 3 ",
                        " MODEL S ",
                        " C5 ",
                        " CORDOBA ");

                case "HATCHBACK" -> containsAny(title,
                        " HATCHBACK ",
                        " SPACEBACK ",
                        " CLIO ",
                        " FABIA ",
                        " SCALA ",
                        " POLO ",
                        " GOLF ",
                        " FIESTA ",
                        " CORSA ",
                        " I20 ",
                        " I30 ",
                        " CEED ",
                        " MAZDA 3 ",
                        " CIVIC ",
                        " MEGANE ",
                        " C2 ",
                        " C3 ",
                        " XSARA ",
                        " AGILA ",
                        " 207 ",
                        " TRIDY A ",
                        " TRIDY-A ",
                        " A-CLASS ");

                case "WAGON" -> containsAny(title,
                        " KOMBI ",
                        " COMBI ",
                        " WAGON ",
                        " VARIANT ",
                        " TOURING ",
                        " AVANT ",
                        " ESTATE ",
                        " GRANDTOUR ",
                        " GRAND TOUR ",
                        " SPORTS TOURER ",
                        " SPORTSWAGON ",
                        " SPORTWAGON ",
                        " CARAVAN ",
                        " SHOOTING BRAKE ",
                        " ALLTRACK ",
                        " SCOUT ",
                        " SW ",
                        " V40 ",
                        " V50 ",
                        " V60 ",
                        " V70 ",
                        " V90 ");

                case "SUV" -> containsAny(title,
                        " SUV ",
                        " CROSSOVER ",
                        " YETI ",
                        " KAMIQ ",
                        " KAROQ ",
                        " KODIAQ ",
                        " T-ROC ",
                        " TROC ",
                        " TIGUAN ",
                        " TOUAREG ",
                        " QASHQAI ",
                        " JUKE ",
                        " X-TRAIL ",
                        " FORMENTOR ",
                        " ATECA ",
                        " ARONA ",
                        " KUGA ",
                        " PUMA ",
                        " ECOSPORT ",
                        " X1 ",
                        " X2 ",
                        " X3 ",
                        " X4 ",
                        " X5 ",
                        " X6 ",
                        " X7 ",
                        " GLA ",
                        " GLB ",
                        " GLC ",
                        " GLE ",
                        " GLS ",
                        " Q2 ",
                        " Q3 ",
                        " Q4 ",
                        " Q5 ",
                        " Q7 ",
                        " Q8 ",
                        " XC40 ",
                        " XC60 ",
                        " XC90 ",
                        " EX30 ",
                        " EX40 ",
                        " EX90 ",
                        " CAPTUR ",
                        " AUSTRAL ",
                        " RAFALE ",
                        " SPORTAGE ",
                        " SORENTO ",
                        " STONIC ",
                        " TUCSON ",
                        " SANTA FE ",
                        " KONA ",
                        " DUSTER ",
                        " KOLEOS ",
                        " KADJAR ",
                        " CR-V ",
                        " HR-V ",
                        " RAV4 ",
                        " C-HR ",
                        " CHR ",
                        " MACAN ",
                        " CAYENNE ",
                        " UX ",
                        " NX ",
                        " RX ",
                        " ENYAQ ",
                        " ID.4 ",
                        " ID.5 ",
                        " EVOQUE ",
                        " VELAR ",
                        " RANGE ROVER ",
                        " DISCOVERY SPORT ",
                        " DEFENDER ",
                        " EV6 ",
                        " 2008 ");

                case "MINIVAN" -> containsAny(title,
                        " MINIVAN ",
                        " MPV ",
                        " CADDY ",
                        " TOURAN ",
                        " SHARAN ",
                        " ALHAMBRA ",
                        " SCENIC ",
                        " ESPACE ",
                        " BERLINGO ",
                        " RIFTER ",
                        " PARTNER TEPEE ",
                        " ZAFIRA ",
                        " MERIVA ",
                        " B-MAX ",
                        " S-MAX ",
                        " GALAXY ",
                        " ROOMSTER ",
                        " LODGY ",
                        " VERSO ",
                        " C-MAX ",
                        " GRAND C-MAX ",
                        " TOURNEO COURIER ",
                        " TOURNEO CONNECT ",
                        " DOBLO ",
                        " COMBO ",
                        " VANEO ");

                case "COUPE" -> containsAny(title,
                        " COUPE ",
                        " COUPÉ ",
                        " GRAN COUPE ",
                        " MUSTANG ",
                        " AMG GT ",
                        " SUPRA ",
                        " BRZ ",
                        " GT86 ",
                        " GR86 ",
                        " 370Z ",
                        " 350Z ",
                        " RC F ",
                        " TT COUPE ",
                        " R8 ");

                case "CABRIO" -> containsAny(title,
                        " CABRIO ",
                        " CABRIOLET ",
                        " KABRIO ",
                        " ROADSTER ",
                        " SPYDER ",
                        " SPIDER ",
                        " CONVERTIBLE ",
                        " MX-5 ",
                        " Z4 ",
                        " SLK ",
                        " BOXSTER ");

                case "PICKUP" -> containsAny(title,
                        " PICKUP ",
                        " PICK-UP ",
                        " RANGER ",
                        " HILUX ",
                        " AMAROK ",
                        " NAVARA ",
                        " L200 ",
                        " GLADIATOR ",
                        " DODGE RAM ",
                        " RAM 1500 ",
                        " RAM 2500 ");

                default -> false;
            };

            if (matchedByTitle) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesBrand(CarEntity car, UserFilterEntity filter) {
        String filterBrands = filter.getBrand();

        if (isBlank(filterBrands) || "ANY".equalsIgnoreCase(filterBrands.trim())) {
            return true;
        }

        String title = " " + normalizeText(car.getTitle()) + " ";
        String carBrand = normalizeToken(car.getBrand());

        for (String rawBrand : filterBrands.split(",")) {
            String brand = normalizeToken(rawBrand);

            if (brand.isBlank()) {
                continue;
            }

            if (!carBrand.isBlank() && (carBrand.equals(brand) || carBrand.contains(brand) || brand.contains(carBrand))) {
                return true;
            }

            if (brand.equals("SKODA")) {
                if (containsAny(title, " SKODA ", " OCTAVIA ", " SUPERB ", " FABIA ", " RAPID ", " KODIAQ ", " KAROQ ", " KAMIQ ", " YETI ", " ENYAQ ")) {
                    return true;
                }
                continue;
            }

            if (brand.equals("VOLKSWAGEN")) {
                if (containsAny(title, " VOLKSWAGEN ", " VW ", " GOLF ", " PASSAT ", " TIGUAN ", " TOUAREG ", " POLO ", " T-ROC ", " TROC ", " BORA ")) {
                    return true;
                }
                continue;
            }

            if (brand.equals("MERCEDES")) {
                if (containsAny(title, " MERCEDES ", " BENZ ", " GLA ", " GLB ", " GLC ", " GLE ", " GLS ", " TRIDY A ", " C-KLASA ", " E-KLASA ")) {
                    return true;
                }
                continue;
            }

            if (brand.equals("LAND_ROVER")) {
                if (containsAny(title, " LAND ROVER ", " RANGE ROVER ", " DEFENDER ", " DISCOVERY ", " EVOQUE ", " VELAR ")) {
                    return true;
                }
                continue;
            }

            if (brand.equals("CITROEN")) {
                if (containsAny(title, " CITROEN ", " C2 ", " C3 ", " C4 ", " C5 ", " XSARA ", " BERLINGO ")) {
                    return true;
                }
                continue;
            }

            if (brand.equals("ALFA_ROMEO")) {
                if (containsAny(title, " ALFA ROMEO ", " GIULIETTA ", " GIULIA ", " STELVIO ", " MITO ", " 156 ", " 159 ")) {
                    return true;
                }
                continue;
            }

            if (brand.equals("RENAULT")) {
                if (containsAny(title, " RENAULT ", " CLIO ", " MEGANE ", " SCENIC ",
                        " GRAND SCENIC ", " LAGUNA ", " KANGOO ", " CAPTUR ",
                        " AUSTRAL ", " RAFALE ", " KOLEOS ", " KADJAR ",
                        " TALISMAN ", " FLUENCE ", " LATITUDE ", " SYMBOL ",
                        " TWINGO ", " ZOE ", " DUSTER ", " SANDERO ", " LOGAN ")) {
                    return true;
                }
                continue;
            }

            if (title.contains(" " + brand + " ")) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesMaxPrice(CarEntity car, UserFilterEntity filter) {
        Integer maxPrice = filter.getMaxPrice();
        Integer priceValue = car.getPriceValue();

        if (maxPrice == null) {
            return true;
        }

        if (priceValue == null) {
            return false;
        }

        return priceValue <= maxPrice;
    }

    private boolean matchesLocation(CarEntity car, UserFilterEntity filter) {
        String filterLocation = filter.getLocation();

        if (isBlank(filterLocation) || "ANY".equalsIgnoreCase(filterLocation.trim())) {
            return true;
        }

        String carLocation = normalizeLocation(car.getLocation());
        String wanted = normalizeLocation(filterLocation);

        if (carLocation.isBlank()) {
            return false;
        }

        if (carLocation.equals(wanted)) {
            return true;
        }

        if (carLocation.contains(wanted) || wanted.contains(carLocation)) {
            return true;
        }

        return sameRegionFamily(carLocation, wanted);
    }

    private boolean sameRegionFamily(String a, String b) {
        if (a.isBlank() || b.isBlank()) {
            return false;
        }

        return (a.contains("PRAHA") && b.contains("PRAHA"))
                || (a.contains("BRNO") && b.contains("JIHOMORAV"))
                || (b.contains("BRNO") && a.contains("JIHOMORAV"))
                || (a.contains("OSTRAVA") && b.contains("MORAVSKOSLEZ"))
                || (b.contains("OSTRAVA") && a.contains("MORAVSKOSLEZ"))
                || (a.contains("PLZEN") && b.contains("PLZENSK"))
                || (b.contains("PLZEN") && a.contains("PLZENSK"))
                || (a.contains("LIBEREC") && b.contains("LIBERECK"))
                || (b.contains("LIBEREC") && a.contains("LIBERECK"))
                || (a.contains("OLOMOUC") && b.contains("OLOMOUCK"))
                || (b.contains("OLOMOUC") && a.contains("OLOMOUCK"))
                || (a.contains("PARDUBICE") && b.contains("PARDUBICK"))
                || (b.contains("PARDUBICE") && a.contains("PARDUBICK"))
                || (a.contains("HRADEC KRALOVE") && b.contains("KRALOVEHRADECK"))
                || (b.contains("HRADEC KRALOVE") && a.contains("KRALOVEHRADECK"))
                || (a.contains("USTI NAD LABEM") && b.contains("USTECK"))
                || (b.contains("USTI NAD LABEM") && a.contains("USTECK"))
                || (a.contains("ZLIN") && b.contains("ZLINSK"))
                || (b.contains("ZLIN") && a.contains("ZLINSK"))
                || (a.contains("JIHLAVA") && b.contains("VYSOCINA"))
                || (b.contains("JIHLAVA") && a.contains("VYSOCINA"))
                || (a.contains("KARLOVY VARY") && b.contains("KARLOVAR"))
                || (b.contains("KARLOVY VARY") && a.contains("KARLOVAR"))
                || (a.contains("CESKE BUDEJOVICE") && b.contains("JIHOCESK"))
                || (b.contains("CESKE BUDEJOVICE") && a.contains("JIHOCESK"))
                || (a.contains("KLADNO") && b.contains("STREDOCESK"))
                || (b.contains("KLADNO") && a.contains("STREDOCESK"))
                || (a.contains("MLADA BOLESLAV") && b.contains("STREDOCESK"))
                || (b.contains("MLADA BOLESLAV") && a.contains("STREDOCESK"))
                || (a.contains("KOLIN") && b.contains("STREDOCESK"))
                || (b.contains("KOLIN") && a.contains("STREDOCESK"));
    }

    private boolean matchesMaxMileage(CarEntity car, UserFilterEntity filter) {
        Integer maxMileage = filter.getMaxMileage();

        if (maxMileage == null) {
            return true;
        }

        Integer carMileage = car.getMileage();

        if (carMileage == null) {
            return true;
        }

        return carMileage <= maxMileage;
    }

    private boolean matchesFuelType(CarEntity car, UserFilterEntity filter) {
        String wantedFuelType = filter.getFuelType();

        if (isBlank(wantedFuelType) || "ANY".equalsIgnoreCase(wantedFuelType.trim())) {
            return true;
        }

        String carFuelType = normalizeToken(car.getFuelType());
        String wanted = normalizeToken(wantedFuelType);

        if (carFuelType.isBlank()) {
            return false;
        }

        if (carFuelType.equals(wanted) || carFuelType.contains(wanted) || wanted.contains(carFuelType)) {
            return true;
        }

        if ("HYBRID".equals(wanted) && "PLUGIN_HYBRID".equals(carFuelType)) {
            return true;
        }

        return false;
    }

    private boolean matchesTransmission(CarEntity car, UserFilterEntity filter) {
        String wantedTransmission = filter.getTransmission();

        if (isBlank(wantedTransmission) || "ANY".equalsIgnoreCase(wantedTransmission.trim())) {
            return true;
        }

        String carTransmission = normalizeToken(car.getTransmission());
        String wanted = normalizeToken(wantedTransmission);

        if (carTransmission.isBlank()) {
            return false;
        }

        return carTransmission.equals(wanted) || carTransmission.contains(wanted) || wanted.contains(carTransmission);
    }

    private boolean matchesYearFrom(CarEntity car, UserFilterEntity filter) {
        Integer yearFrom = filter.getYearFrom();

        if (yearFrom == null) {
            return true;
        }

        Integer carYear = car.getYear();

        if (carYear == null) {
            return true;
        }

        return carYear >= yearFrom;
    }

    private boolean looksClearlyOldCarTitle(String title) {
        return containsAny(title,
                " PASSAT B5 ",
                " PASSAT VARIANT 1.9TDI 74KW ",
                " PASSAT 1.6 FSI ",
                " BORA ",
                " GOLF 1,4 BASIS ",
                " GOLF 1.4 BASIS ",
                " FELICIA ",
                " FAVORIT ",
                " FORMAN ",
                " CORDOBA ",
                " XSARA ",
                " XANTIA ",
                " SAXO ",
                " C2 1,1 ",
                " C3 1,4 ",
                " ASTRA G ",
                " ASTRA CARAVAN 1.7CDTI ",
                " OCTAVIA TOUR ",
                " OCTAVIA I ",
                " OCTAVIA 1 ",
                " FABIA I ",
                " FABIA 1 ",
                " SCENIC I ",
                " SCENIC 1 ",
                " CLIO 1 ",
                " CLIO 2 ",
                " MEGANE 2,0 ",
                " MEGANE 1.4I ",
                " E39 ",
                " E46 ",
                " A190 LONG ",
                " GETZ 1.1 ",
                " MERIVA 1.4 ",
                " ZAFIRA 1,8 ",
                " FOCUS 1,6 I ",
                " FOCUS 1.6I ",
                " FOCUS 1,8 16V ",
                " ALFA ROMEO 156 ",
                " 1.9 TDI 66KW ",
                " 1.9TDI 74KW ");
    }

    private String normalizeLocation(String value) {
        String normalized = normalizeText(value);

        normalized = normalized
                .replace("HLAVNI MESTO ", "")
                .replace("HLAVNI MESTO PRAHA", "PRAHA")
                .replace("KRAJ", "")
                .replace("OKRES", "")
                .replace("CESKA REPUBLIKA", "")
                .replaceAll("\\b\\d{3}\\s?\\d{2}\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.equals("PRAHA")) return "PRAHA";
        if (normalized.startsWith("PRAHA ")) return "PRAHA";
        if (normalized.equals("BRNO-MESTO")) return "BRNO";
        if (normalized.equals("BRNO VENKOV")) return "BRNO";
        if (normalized.equals("TEPLICE CITRO")) return "TEPLICE";

        return normalized;
    }

    private String normalizeToken(String value) {
        return normalizeText(value)
                .replace('-', '_')
                .replace(' ', '_')
                .trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String upper = value.trim().toUpperCase(Locale.ROOT);
        String normalized = Normalizer.normalize(upper, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.replaceAll("\\s+", " ").trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean containsAny(String source, String... values) {
        if (source == null || source.isBlank()) {
            return false;
        }

        for (String value : values) {
            if (value != null && !value.isBlank() && source.contains(value)) {
                return true;
            }
        }

        return false;
    }
}