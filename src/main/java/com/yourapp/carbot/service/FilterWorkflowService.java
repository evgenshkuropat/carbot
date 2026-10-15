package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.UserFilterEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FilterWorkflowService {

    private final UserFilterService userFilterService;
    private final Set<Long> editingChats = ConcurrentHashMap.newKeySet();

    public FilterWorkflowService(UserFilterService userFilterService) {
        this.userFilterService = userFilterService;
    }

    public UserFilterEntity beginSetup(Long chatId, String language) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLanguageCode(language);
        userFilterService.save(filter);
        return filter;
    }

    public UserFilterEntity getOrCreate(Long chatId) {
        return userFilterService.getOrCreate(chatId);
    }

    public void beginEditing(Long chatId) {
        editingChats.add(chatId);
    }

    public void finishEditing(Long chatId) {
        editingChats.remove(chatId);
    }

    public boolean isEditing(Long chatId) {
        return editingChats.contains(chatId);
    }

    public UserFilterEntity updateModelQuery(Long chatId, String value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setModelQuery(hasText(value) ? value : null);
        save(filter);
        return filter;
    }

    public UserFilterEntity clearModelQuery(Long chatId) {
        return updateModelQuery(chatId, null);
    }

    public UserFilterEntity toggleCarType(Long chatId, String carType) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setCarType(toggle(filter.getCarType(), carType));
        save(filter);
        return filter;
    }

    public UserFilterEntity clearCarTypes(Long chatId, boolean clearBrand) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setCarType(null);
        if (clearBrand) filter.setBrand(null);
        save(filter);
        return filter;
    }

    public UserFilterEntity finishCarTypes(Long chatId, boolean clearBrand) {
        UserFilterEntity filter = getOrCreate(chatId);
        if (clearBrand) filter.setBrand(null);
        save(filter);
        return filter;
    }

    public UserFilterEntity toggleBrand(Long chatId, String brand) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setBrand(toggle(filter.getBrand(), brand));
        save(filter);
        return filter;
    }

    public UserFilterEntity clearBrands(Long chatId, boolean clearModel) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setBrand(null);
        if (clearModel) filter.setModelQuery(null);
        save(filter);
        return filter;
    }

    public UserFilterEntity finishBrands(Long chatId, boolean clearModel) {
        UserFilterEntity filter = getOrCreate(chatId);
        if (clearModel) filter.setModelQuery(null);
        save(filter);
        return filter;
    }

    public UserFilterEntity setMaxPrice(Long chatId, Integer value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setMaxPrice(value == null || value == 0 ? null : value);
        save(filter);
        return filter;
    }

    public UserFilterEntity setLocation(Long chatId, String value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setLocation("ANY".equals(value) ? null : value);
        save(filter);
        return filter;
    }

    public UserFilterEntity setMaxMileage(Long chatId, Integer value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setMaxMileage(value == null || value == 0 ? null : value);
        save(filter);
        return filter;
    }

    public UserFilterEntity setTransmission(Long chatId, String value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setTransmission("ANY".equals(value) ? null : value);
        save(filter);
        return filter;
    }

    public UserFilterEntity setFuelType(Long chatId, String value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setFuelType("ANY".equals(value) ? null : value);
        save(filter);
        return filter;
    }

    public UserFilterEntity setYearFrom(Long chatId, Integer value) {
        UserFilterEntity filter = getOrCreate(chatId);
        filter.setYearFrom(value == null || value == 0 ? null : value);
        save(filter);
        return filter;
    }

    private void save(UserFilterEntity filter) {
        userFilterService.save(filter);
    }

    private String toggle(String rawValues, String value) {
        Set<String> values = parseValues(rawValues);
        if (!values.add(value)) values.remove(value);
        return String.join(",", values);
    }

    private Set<String> parseValues(String rawValues) {
        Set<String> values = new LinkedHashSet<>();
        if (rawValues == null || rawValues.isBlank()) return values;
        for (String value : rawValues.split(",")) {
            if (hasText(value)) values.add(value.trim());
        }
        return values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
