package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.UserFilterEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilterWorkflowServiceTest {

    private final UserFilterService userFilterService = mock(UserFilterService.class);
    private final FilterWorkflowService service = new FilterWorkflowService(userFilterService);

    @Test
    void togglesMultipleCarTypesWithoutDuplicates() {
        UserFilterEntity filter = filter();
        when(userFilterService.getOrCreate(11L)).thenReturn(filter);

        service.toggleCarType(11L, "SUV");
        service.toggleCarType(11L, "WAGON");
        service.toggleCarType(11L, "SUV");

        assertThat(filter.getCarType()).isEqualTo("WAGON");
        verify(userFilterService).save(filter);
    }

    @Test
    void clearsDependentBrandWhenCompletingNewCarTypeSelection() {
        UserFilterEntity filter = filter();
        filter.setBrand("SKODA");
        when(userFilterService.getOrCreate(11L)).thenReturn(filter);

        service.finishCarTypes(11L, true);

        assertThat(filter.getBrand()).isNull();
    }

    @Test
    void tracksEditModePerChat() {
        service.beginEditing(11L);

        assertThat(service.isEditing(11L)).isTrue();

        service.finishEditing(11L);
        assertThat(service.isEditing(11L)).isFalse();
    }

    private UserFilterEntity filter() {
        UserFilterEntity filter = new UserFilterEntity();
        filter.setChatId(11L);
        return filter;
    }
}
