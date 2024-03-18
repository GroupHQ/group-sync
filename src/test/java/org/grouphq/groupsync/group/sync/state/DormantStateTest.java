package org.grouphq.groupsync.group.sync.state;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.config.ClientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class DormantStateTest {

    @Mock
    private ClientProperties clientProperties;

    @Mock
    private GroupInitialStateService groupInitialStateService;

    @InjectMocks
    private DormantState dormantState;

    @Test
    @DisplayName("Should return the same hot source when called multiple times")
    void shouldReturnTheSameHotSourceWhenCalledMultipleTimes() {
        given(groupInitialStateService.initializeGroupState()).willReturn(Mono.empty());

        final Mono<Void> firstRequest = dormantState.onRequest();
        final Mono<Void> secondRequest = dormantState.onRequest();

        StepVerifier.create(firstRequest.then(secondRequest))
            .expectComplete()
            .verify();

        verify(groupInitialStateService, times(1)).setState(Mockito.any(ReadyState.class));
    }
}
