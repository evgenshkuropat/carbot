package com.yourapp.carbot.service;

import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import com.yourapp.carbot.repository.UserFilterRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminStatsService {

    private final CarRepository carRepository;
    private final TelegramSubscriberRepository subscriberRepository;
    private final UserFilterRepository userFilterRepository;

    public AdminStatsService(CarRepository carRepository,
                             TelegramSubscriberRepository subscriberRepository,
                             UserFilterRepository userFilterRepository) {
        this.carRepository = carRepository;
        this.subscriberRepository = subscriberRepository;
        this.userFilterRepository = userFilterRepository;
    }

    public String buildStatsText() {
        long totalCars = carRepository.count();
        long bazosCars = carRepository.countBySource("BAZOS");
        long sautoCars = carRepository.countBySource("SAUTO");
        long tipcarsCars = carRepository.countBySource("TIPCARS");

        long totalUsers = subscriberRepository.count();
        long configuredFilters = userFilterRepository.count();

        return """
                📊 Admin статистика

                👥 Пользователи:
                • Всего пользователей: %d
                • Фильтров в базе: %d

                🚗 Авто в базе:
                • Всего: %d
                • Bazoš: %d
                • Sauto: %d
                • TipCars: %d

                ✅ Бот работает.
                """.formatted(
                totalUsers,
                configuredFilters,
                totalCars,
                bazosCars,
                sautoCars,
                tipcarsCars
        );
    }
}