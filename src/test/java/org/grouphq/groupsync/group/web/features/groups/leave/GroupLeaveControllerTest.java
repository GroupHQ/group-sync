package org.grouphq.groupsync.group.web.features.groups.leave;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.group.web.features.GroupLeaveController;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupLeaveRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.annotation.SecurityTestExecutionListeners;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@WithMockUser
@SecurityTestExecutionListeners
@Tag("UnitTest")
class GroupLeaveControllerTest {

    @Spy
    private UserService userService;

    @Mock
    private GroupEventPublisher groupEventPublisher;

    @InjectMocks
    GroupLeaveController groupSocketController;

    private static final String INTERNAL_SERVER_ERROR_SUFFIX = """
             because the server has encountered an unexpected error.
            Rest assured, this will be investigated.
            """;
    private static final String DUMMY_MESSAGE = "This message should NOT be returned to the user!";

    @Test
    @DisplayName("Test RSocket integration for leaving a group")
    void testLeaveGroup() {
        final GroupLeaveRequestEvent originalRequest = GroupTestUtility.generateGroupLeaveRequestEvent();

        given(groupEventPublisher.publishGroupLeaveRequest(
            argThat(publishedRequest -> originalRequest.getAggregateId().equals(publishedRequest.getAggregateId())
                    && originalRequest.getMemberId().equals(publishedRequest.getMemberId())
                    && !originalRequest.getEventId().equals(publishedRequest.getEventId())
                    && !originalRequest.getCreatedDate().equals(publishedRequest.getCreatedDate())
                    && !originalRequest.getWebsocketId().equals(publishedRequest.getWebsocketId())
            ))
        ).willReturn(Mono.empty());

        StepVerifier.create(groupSocketController.leaveGroup(originalRequest))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for returning an error when leaving a group")
    void testLeaveGroupError() {
        final GroupLeaveRequestEvent event = GroupTestUtility.generateGroupLeaveRequestEvent();

        given(groupEventPublisher.publishGroupLeaveRequest(any(GroupLeaveRequestEvent.class)))
                .willReturn(Mono.error(new RuntimeException(DUMMY_MESSAGE)));

        final String expectedErrorString = "Cannot leave group" + INTERNAL_SERVER_ERROR_SUFFIX;

        StepVerifier.create(groupSocketController.leaveGroup(event))
                .expectErrorMatches(throwable -> throwable instanceof InternalServerError
                        && throwable.getMessage().equals(expectedErrorString))
                .verify();
    }
}
