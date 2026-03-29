package com.yourapp.carbot.controller;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.CarParserService;
import com.yourapp.carbot.service.dto.CarDto;
import com.yourapp.carbot.service.dto.CarResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CarController {

    private final CarParserService carParserService;
    private final CarRepository carRepository;

    public CarController(CarParserService carParserService,
                         CarRepository carRepository) {
        this.carParserService = carParserService;
        this.carRepository = carRepository;
    }

    @GetMapping("/api/cars")
    public List<CarDto> getCars() {
        return carParserService.findCars();
    }

    @GetMapping("/api/cars/save")
    public String saveCars() {
        return "Saved new cars: " + carParserService.fetchAndStoreCars().size();
    }

    @GetMapping("/api/cars/db")
    public List<CarResponseDto> getCarsFromDb() {
        return carRepository.findAll()
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

    @GetMapping("/api/cars/db/filter")
    public List<CarResponseDto> filterCars(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String query
    ) {
        return carRepository.findTop200ByOrderByCreatedAtDesc()
                .stream()
                .filter(car -> source == null || source.isBlank() ||
                        (car.getSource() != null && car.getSource().equalsIgnoreCase(source)))
                .filter(car -> location == null || location.isBlank() ||
                        (car.getLocation() != null && car.getLocation().toLowerCase().contains(location.toLowerCase())))
                .filter(car -> maxPrice == null ||
                        (car.getPriceValue() != null && car.getPriceValue() <= maxPrice))
                .filter(car -> query == null || query.isBlank() ||
                        (car.getTitle() != null && car.getTitle().toLowerCase().contains(query.toLowerCase())))
                .map(this::toDto)
                .toList();
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
                car.getCreatedAt()
        );
    }
}