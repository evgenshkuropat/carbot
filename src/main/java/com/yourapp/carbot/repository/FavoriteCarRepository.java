package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.FavoriteCarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteCarRepository extends JpaRepository<FavoriteCarEntity, Long> {

    boolean existsByChatIdAndCarId(Long chatId, Long carId);

    Optional<FavoriteCarEntity> findByChatIdAndCarId(Long chatId, Long carId);

    List<FavoriteCarEntity> findByChatIdOrderByCreatedAtDesc(Long chatId);

    long deleteByChatIdAndCarId(Long chatId, Long carId);
}