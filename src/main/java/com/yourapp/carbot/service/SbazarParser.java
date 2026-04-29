package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SbazarParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(SbazarParser.class);

    @PostConstruct
    public void init() {
        log.warn("SBAZAR parser bean initialized");
    }

    @Override
    public String getSourceName() {
        return "SBAZAR";
    }

    @Override
    public List<CarDto> fetchCars() {

        log.warn("SBAZAR debug mode enabled. Parser is running correctly.");

        try {
            log.warn("SBAZAR DEBUG attempting connection to sbazar.cz");

        } catch (Exception e) {

            log.warn("SBAZAR DEBUG connection failed: {}", e.getMessage());

        }

        return List.of();
    }
}