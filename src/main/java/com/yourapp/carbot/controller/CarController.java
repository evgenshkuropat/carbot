package com.yourapp.carbot.controller;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.CarParserService;
import com.yourapp.carbot.service.dto.CarResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class CarController {

    private final CarParserService carParserService;
    private final CarRepository carRepository;
    private final String apiAdminToken;

    public CarController(CarParserService carParserService,
                         CarRepository carRepository,
                         @Value("${api.admin-token:}") String apiAdminToken) {
        this.carParserService = carParserService;
        this.carRepository = carRepository;
        this.apiAdminToken = apiAdminToken;
    }

    @GetMapping({"/api/cars", "/api/cars/db/filter"})
    public Page<CarResponseDto> getCars(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));

        return carRepository.findFiltered(
                        blankToNull(source),
                        blankToNull(location),
                        maxPrice,
                        blankToNull(query),
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(this::toDto);
    }

    @PostMapping("/api/cars/sync")
    public String saveCars(@RequestHeader(value = "X-API-ADMIN-TOKEN", required = false) String adminToken) {
        requireAdminToken(adminToken);
        return "Saved new cars: " + carParserService.fetchAndStoreCars().size();
    }

    @GetMapping("/api/cars/db")
    public List<CarResponseDto> getCarsFromDb() {
        return carRepository.findTop200ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/latest")
    public List<CarResponseDto> getLatestCarsFromDb() {
        return carRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/source/{source}")
    public List<CarResponseDto> getCarsBySource(@PathVariable String source) {
        return carRepository.findTop50BySourceOrderByCreatedAtDesc(source.toUpperCase())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/location/{location}")
    public List<CarResponseDto> getCarsByLocation(@PathVariable String location) {
        return carRepository.findTop50ByLocationContainingIgnoreCaseOrderByCreatedAtDesc(location)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/price/max/{maxPrice}")
    public List<CarResponseDto> getCarsByMaxPrice(@PathVariable Integer maxPrice) {
        return carRepository.findTop50ByPriceValueLessThanEqualOrderByCreatedAtDesc(maxPrice)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/price/asc")
    public List<CarResponseDto> getCarsByPriceAsc() {
        return carRepository.findTop50ByOrderByPriceValueAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/price/desc")
    public List<CarResponseDto> getCarsByPriceDesc() {
        return carRepository.findTop50ByOrderByPriceValueDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/api/cars/db/search/{query}")
    public List<CarResponseDto> searchCarsByTitle(@PathVariable String query) {
        return carRepository.findTop50ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(query)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void requireAdminToken(String token) {
        if (apiAdminToken == null || apiAdminToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (token == null || !apiAdminToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private CarResponseDto toDto(CarEntity car) {
        return new CarResponseDto(
                car.getId(),
                car.getSource(),
                car.getTitle(),
                car.getPrice(),
                car.getPriceValue(),
                car.getLocation(),
                car.getUrl(),
                car.getImageUrl(),
                car.getBrand(),
                car.getYear(),
                car.getMileage(),
                car.getFuelType(),
                car.getTransmission(),
                car.getCarType(),
                car.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
