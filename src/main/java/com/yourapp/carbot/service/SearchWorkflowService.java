package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SearchWorkflowService {

    private final CarSearchService carSearchService;
    private final UserFilterService userFilterService;
    private final Map<Long, SearchSession> sessions = new ConcurrentHashMap<>();

    public SearchWorkflowService(CarSearchService carSearchService, UserFilterService userFilterService) {
        this.carSearchService = carSearchService;
        this.userFilterService = userFilterService;
    }

    public StartResult start(Long chatId, int limit) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);
        if (!userFilterService.isConfigured(filter)) {
            sessions.remove(chatId);
            return StartResult.filterMissing();
        }

        List<CarEntity> cars = carSearchService.findMatchingCars(chatId, limit);
        if (cars.isEmpty()) {
            sessions.remove(chatId);
            return StartResult.empty(filter);
        }

        sessions.put(chatId, new SearchSession(cars));
        return StartResult.ready(filter, cars);
    }

    public Optional<SearchItem> current(Long chatId) {
        return session(chatId).map(SearchSession::currentItem);
    }

    public Optional<SearchItem> next(Long chatId) {
        return session(chatId).map(session -> {
            session.next();
            return session.currentItem();
        });
    }

    public Optional<SearchItem> previous(Long chatId) {
        return session(chatId).map(session -> {
            session.previous();
            return session.currentItem();
        });
    }

    public void stop(Long chatId) {
        sessions.remove(chatId);
    }

    private Optional<SearchSession> session(Long chatId) {
        SearchSession session = sessions.get(chatId);
        return session == null || session.cars.isEmpty() ? Optional.empty() : Optional.of(session);
    }

    public record StartResult(Status status, UserFilterEntity filter, List<CarEntity> cars) {
        public static StartResult filterMissing() { return new StartResult(Status.FILTER_MISSING, null, List.of()); }
        public static StartResult empty(UserFilterEntity filter) { return new StartResult(Status.EMPTY, filter, List.of()); }
        public static StartResult ready(UserFilterEntity filter, List<CarEntity> cars) { return new StartResult(Status.READY, filter, cars); }
        public enum Status { FILTER_MISSING, EMPTY, READY }
    }

    public record SearchItem(CarEntity car, int number, int total, boolean hasPrevious, boolean hasNext) { }

    private static final class SearchSession {
        private final List<CarEntity> cars;
        private int index;

        private SearchSession(List<CarEntity> cars) {
            this.cars = List.copyOf(cars);
        }

        private void next() {
            if (index < cars.size() - 1) index++;
        }

        private void previous() {
            if (index > 0) index--;
        }

        private SearchItem currentItem() {
            return new SearchItem(cars.get(index), index + 1, cars.size(), index > 0, index < cars.size() - 1);
        }
    }
}
