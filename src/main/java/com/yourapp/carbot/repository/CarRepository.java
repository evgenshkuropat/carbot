package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.CarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<CarEntity> findAllByListingStatusOrderByCreatedAtDesc(String listingStatus);

    List<CarEntity> findAllByListingStatusAndCreatedAtAfterOrderByCreatedAtDesc(String listingStatus, LocalDateTime dateTime);

    List<CarEntity> findTop5ByOrderByCreatedAtDesc();

    List<CarEntity> findTop10ByListingStatusOrderByCreatedAtDesc(String listingStatus);

    List<CarEntity> findTop50ByOrderByCreatedAtDesc();

    List<CarEntity> findTop50ByOwnerChatIdOrderByCreatedAtDesc(Long ownerChatId);

    List<CarEntity> findTop50BySourceOrderByCreatedAtDesc(String source);

    List<CarEntity> findTop50ByLocationContainingIgnoreCaseOrderByCreatedAtDesc(String location);

    List<CarEntity> findTop50ByPriceValueLessThanEqualOrderByCreatedAtDesc(Integer priceValue);

    List<CarEntity> findTop50ByOrderByPriceValueAsc();

    List<CarEntity> findTop50ByOrderByPriceValueDesc();

    List<CarEntity> findTop50ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    List<CarEntity> findTop200ByOrderByCreatedAtDesc();

    @Query("""
            select c
            from CarEntity c
            where (:source is null or lower(c.source) = lower(:source))
              and (:location is null or lower(c.location) like lower(concat('%', :location, '%')))
              and (:maxPrice is null or c.priceValue <= :maxPrice)
              and (:query is null or lower(c.title) like lower(concat('%', :query, '%')))
            """)
    Page<CarEntity> findFiltered(
            @Param("source") String source,
            @Param("location") String location,
            @Param("maxPrice") Integer maxPrice,
            @Param("query") String query,
            Pageable pageable
    );

    long countBySource(String source);

    long countByListingStatus(String listingStatus);

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
