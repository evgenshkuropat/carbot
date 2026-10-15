package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminDashboardService {

    private final CarRepository carRepository;
    private final TelegramSubscriberService subscriberService;
    private final FavoriteCarService favoriteCarService;

    public AdminDashboardService(CarRepository carRepository,
                                 TelegramSubscriberService subscriberService,
                                 FavoriteCarService favoriteCarService) {
        this.carRepository = carRepository;
        this.subscriberService = subscriberService;
        this.favoriteCarService = favoriteCarService;
    }

    public DashboardData dashboard(LocalDateTime since) {
        return new DashboardData(
                subscriberService.countAllSubscribers(),
                subscriberService.countActiveSubscribers(),
                favoriteCarService.countAllFavorites(),
                carRepository.count(),
                carRepository.countByCreatedAtAfter(since),
                carRepository.countByListingStatus("PENDING"),
                carRepository.countCarsBySource(),
                carRepository.countCarsBySourceAfter(since),
                carRepository.findTop5ByOrderByCreatedAtDesc()
        );
    }

    public List<CarEntity> pendingListings() {
        return carRepository.findTop10ByListingStatusOrderByCreatedAtDesc("PENDING");
    }

    public OwnerListings ownerListings() {
        List<CarEntity> cars = carRepository.findTop50BySourceOrderByCreatedAtDesc("USER");
        return new OwnerListings(
                carRepository.countBySource("USER"),
                cars,
                countStatus(cars, "PENDING"),
                countStatus(cars, "ACTIVE"),
                countStatus(cars, "REJECTED"),
                countStatus(cars, "INACTIVE")
        );
    }

    private long countStatus(List<CarEntity> cars, String status) {
        return cars.stream().filter(car -> status.equalsIgnoreCase(car.getListingStatus())).count();
    }

    public record DashboardData(long usersTotal, long usersActive, long favoritesTotal,
                                long totalCars, long newLast24h, long pendingUserListings,
                                List<CarRepository.SourceCount> sourcesTotal,
                                List<CarRepository.SourceCount> sourcesLast24h,
                                List<CarEntity> latestCars) { }

    public record OwnerListings(long total, List<CarEntity> cars, long pending,
                                long active, long rejected, long inactive) { }
}
