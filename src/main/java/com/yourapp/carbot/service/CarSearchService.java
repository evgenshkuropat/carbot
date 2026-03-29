package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarSearchService {

    private final CarRepository carRepository;
    private final UserFilterService userFilterService;
    private final CarFilterMatcher carFilterMatcher;

    public CarSearchService(CarRepository carRepository,
                            UserFilterService userFilterService,
                            CarFilterMatcher carFilterMatcher) {
        this.carRepository = carRepository;
        this.userFilterService = userFilterService;
        this.carFilterMatcher = carFilterMatcher;
    }

    public List<CarEntity> findMatchingCars(Long chatId, int limit) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);

        if (filter == null) {
            return List.of();
        }

        return carRepository.findTop200ByOrderByCreatedAtDesc()
                .stream()
                .filter(car -> carFilterMatcher.matches(car, filter))
                .limit(limit)
                .toList();
    }
}