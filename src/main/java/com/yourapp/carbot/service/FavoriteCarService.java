package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.FavoriteCarEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.repository.FavoriteCarRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FavoriteCarService {

    private final FavoriteCarRepository favoriteCarRepository;
    private final CarRepository carRepository;

    public FavoriteCarService(FavoriteCarRepository favoriteCarRepository,
                              CarRepository carRepository) {
        this.favoriteCarRepository = favoriteCarRepository;
        this.carRepository = carRepository;
    }

    public boolean addToFavorites(Long chatId, Long carId) {

        if (chatId == null || carId == null) {
            return false;
        }

        if (!carRepository.existsById(carId)) {
            return false;
        }

        if (favoriteCarRepository.existsByChatIdAndCarId(chatId, carId)) {
            return false;
        }

        FavoriteCarEntity favorite = new FavoriteCarEntity();
        favorite.setChatId(chatId);
        favorite.setCarId(carId);
        favorite.setCreatedAt(LocalDateTime.now());

        favoriteCarRepository.save(favorite);

        return true;
    }

    public boolean removeFromFavorites(Long chatId, Long carId) {

        if (chatId == null || carId == null) {
            return false;
        }

        long deleted = favoriteCarRepository.deleteByChatIdAndCarId(chatId, carId);

        return deleted > 0;
    }

    public List<CarEntity> getFavorites(Long chatId) {

        List<Long> ids = favoriteCarRepository
                .findByChatIdOrderByCreatedAtDesc(chatId)
                .stream()
                .map(FavoriteCarEntity::getCarId)
                .toList();

        if (ids.isEmpty()) {
            return List.of();
        }

        return carRepository.findAllById(ids);
    }

    public boolean isFavorite(Long chatId, Long carId) {

        if (chatId == null || carId == null) {
            return false;
        }

        return favoriteCarRepository.existsByChatIdAndCarId(chatId, carId);
    }
}