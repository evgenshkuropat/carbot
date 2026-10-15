package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchWorkflowServiceTest {

    private final CarSearchService carSearchService = mock(CarSearchService.class);
    private final UserFilterService userFilterService = mock(UserFilterService.class);
    private final SearchWorkflowService service = new SearchWorkflowService(carSearchService, userFilterService);

    @Test
    void startsSearchAndNavigatesThroughResults() {
        UserFilterEntity filter = new UserFilterEntity();
        CarEntity first = new CarEntity();
        first.setTitle("First");
        CarEntity second = new CarEntity();
        second.setTitle("Second");
        when(userFilterService.findByChatId(7L)).thenReturn(Optional.of(filter));
        when(userFilterService.isConfigured(filter)).thenReturn(true);
        when(carSearchService.findMatchingCars(7L, 50)).thenReturn(List.of(first, second));

        SearchWorkflowService.StartResult result = service.start(7L, 50);

        assertThat(result.status()).isEqualTo(SearchWorkflowService.StartResult.Status.READY);
        assertThat(service.current(7L).orElseThrow().car()).isSameAs(first);
        assertThat(service.next(7L).orElseThrow().car()).isSameAs(second);
        assertThat(service.previous(7L).orElseThrow().car()).isSameAs(first);
    }

    @Test
    void doesNotStartSearchWithoutConfiguredFilter() {
        when(userFilterService.findByChatId(7L)).thenReturn(Optional.empty());
        when(userFilterService.isConfigured(null)).thenReturn(false);

        SearchWorkflowService.StartResult result = service.start(7L, 50);

        assertThat(result.status()).isEqualTo(SearchWorkflowService.StartResult.Status.FILTER_MISSING);
        assertThat(service.current(7L)).isEmpty();
    }
}
