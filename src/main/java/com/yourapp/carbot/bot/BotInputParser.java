package com.yourapp.carbot.bot;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BotInputParser {

    public String command(String text) {
        if (text == null || text.isBlank()) return "";
        return text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    public String callbackData(String data) {
        return data == null ? "" : data.trim();
    }

    public String legacyEditField(String data) {
        return switch (callbackData(data)) {
            case "edit_car_type", "edit:car_type", "filter_edit:car_type" -> "car_type";
            case "edit_brand", "edit:brand", "filter_edit:brand" -> "brand";
            case "edit_model", "edit:model", "edit_model_query", "edit:model_query", "filter_edit:model" -> "model_query";
            case "edit_max_price", "edit:price", "edit:max_price", "filter_edit:max_price" -> "max_price";
            case "edit_location", "edit:location", "filter_edit:location" -> "location";
            case "edit_max_mileage", "edit:mileage", "edit:max_mileage", "filter_edit:max_mileage" -> "max_mileage";
            case "edit_transmission", "edit:transmission", "filter_edit:transmission" -> "transmission";
            case "edit_fuel_type", "edit:fuel_type", "filter_edit:fuel_type" -> "fuel_type";
            case "edit_year_from", "edit:year_from", "filter_edit:year_from" -> "year_from";
            default -> null;
        };
    }
}
