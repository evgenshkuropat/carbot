package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;

import java.util.List;

public interface CarSourceParser {
    String getSourceName();
    List<CarDto> fetchCars();
}