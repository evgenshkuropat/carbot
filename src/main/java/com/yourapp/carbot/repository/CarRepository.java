package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.CarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<CarEntity, Long> {

    Optional<CarEntity> findByUrl(String url);

    List<CarEntity> findByUrlIn(Collection<String> urls);

    List<CarEntity> findAllByOrderByCreatedAtDesc();

    List<CarEntity> findTop5ByOrderByCreatedAtDesc();

    List<CarEntity> findTop50ByOrderByCreatedAtDesc();

    List<CarEntity> findTop50BySourceOrderByCreatedAtDesc(String source);

    List<CarEntity> findTop50ByLocationContainingIgnoreCaseOrderByCreatedAtDesc(String location);

    List<CarEntity> findTop50ByPriceValueLessThanEqualOrderByCreatedAtDesc(Integer priceValue);

    List<CarEntity> findTop50ByOrderByPriceValueAsc();

    List<CarEntity> findTop50ByOrderByPriceValueDesc();

    List<CarEntity> findTop50ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    List<CarEntity> findTop200ByOrderByCreatedAtDesc();

    long countBySource(String source);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countBySourceAndCreatedAtAfter(String source, LocalDateTime dateTime);

    @Query("""
            select c.source as source, count(c) as total
            from CarEntity c
            where c.source is not null and c.source <> ''
            group by c.source
            order by count(c) desc
            """)
    List<SourceCount> countCarsBySource();

    @Query("""
            select c.source as source, count(c) as total
            from CarEntity c
            where c.source is not null and c.source <> '' and c.createdAt > :dateTime
            group by c.source
            order by count(c) desc
            """)
    List<SourceCount> countCarsBySourceAfter(@Param("dateTime") LocalDateTime dateTime);

    interface SourceCount {
        String getSource();

        long getTotal();
    }
}
