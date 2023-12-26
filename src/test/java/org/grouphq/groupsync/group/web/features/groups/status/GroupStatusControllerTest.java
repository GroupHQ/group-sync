package org.grouphq.groupsync.group.web.features.groups.status;

import static org.mockito.BDDMockito.given;

import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.web.features.GroupStatusController;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupStatusRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.annotation.SecurityTestExecutionListeners;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@SecurityTestExecutionListeners
@Tag("UnitTest")
class GroupStatusControllerTest {

    @Mock
    private GroupEventPublisher groupEventPublisher;

    @InjectMocks
    GroupStatusController groupSocketController;

    private static final String INTERNAL_SERVER_ERROR_SUFFIX = """
             because the server has encountered an unexpected error.
            Rest assured, this will be investigated.
            """;
    private static final String DUMMY_MESSAGE = "This message should NOT be returned to the user!";

    @Test
    @DisplayName("Test RSocket integration for updating a group status")
    void testUpdateGroupStatus() {
        final GroupStatusRequestEvent event =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        given(groupEventPublisher.publishGroupUpdateStatusRequest(event)).willReturn(Mono.empty());

        StepVerifier.create(groupSocketController.updateGroupStatus(event))
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for returning an error when updating a group status")
    void testUpdateGroupStatusError() {
        final GroupStatusRequestEvent event =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        given(groupEventPublisher.publishGroupUpdateStatusRequest(event))
            .willReturn(Mono.error(new RuntimeException(DUMMY_MESSAGE)));

        final String expectedErrorString =
            "Cannot update group status" + INTERNAL_SERVER_ERROR_SUFFIX;

        StepVerifier.create(groupSocketController.updateGroupStatus(event))
            .expectErrorMatches(throwable -> throwable instanceof InternalServerError
                    && throwable.getMessage().equals(expectedErrorString))
            .verify();
    }
}
