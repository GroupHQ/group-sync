package org.grouphq.groupsync.group.sync;

import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.GroupServiceUnavailableException;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class GroupServiceClientTest {
    private MockWebServer mockWebServer;

    @Mock
    private ClientProperties clientProperties;


    private GroupServiceClient groupServiceClient;

    private ObjectMapper objectMapper;


    @BeforeEach
    void setup() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        final URI mockWebServerUri = mockWebServer.url("/").uri();
        final WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServerUri.toString())
            .build();

        this.groupServiceClient = new GroupServiceClient(webClient, clientProperties);
    }

    @AfterEach
    void clean() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("When there are active groups, then return a list of active groups")
    void whenActiveGroupsExistThenReturnActiveGroups() throws JsonProcessingException {
        given(clientProperties.getGroupsTimeoutMilliseconds()).willReturn(10_000L);

        final Group[] testGroups = {
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE),
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE)
        };

        final var groupsAsJson = objectMapper.writeValueAsString(testGroups);

        final var mockResponse = new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(groupsAsJson);

        mockWebServer.enqueue(mockResponse);

        final Flux<Group> groups = groupServiceClient.getGroups();

        StepVerifier.create(groups)
            .expectNext(testGroups[0])
            .expectNext(testGroups[1])
            .verifyComplete();
    }

    @Test
    @DisplayName("Timeout and retry appropriately when group service does not respond with groups")
    void whenGroupServiceTimesOutOnGroupsFetchThenReturnException() {
        given(clientProperties.getGroupsTimeoutMilliseconds()).willReturn(1L);

        final var mockResponse = new MockResponse()
            .setSocketPolicy(SocketPolicy.NO_RESPONSE);

        mockWebServer.enqueue(mockResponse);

        final Flux<Group> groups = groupServiceClient.getGroups();

        StepVerifier.create(groups)
            .expectError(GroupServiceUnavailableException.class)
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("When there is a member for a user, then return the member")
    void whenMemberExistsThenReturnMember() throws JsonProcessingException {
        given(clientProperties.getGroupsTimeoutMilliseconds()).willReturn(10_000L);

        final var testMember = Member.of(UUID.randomUUID(), "test-username", 1L);

        final var memberAsJson = objectMapper.writeValueAsString(testMember);

        final var mockResponse = new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(memberAsJson);

        mockWebServer.enqueue(mockResponse);

        final Mono<Member> member = groupServiceClient.getMyMember(testMember.websocketId().toString());

        StepVerifier.create(member)
            .expectNext(testMember)
            .verifyComplete();
    }

    @Test
    @DisplayName("Timeout and retry appropriately when group service does not respond with member for user")
    void whenGroupServiceTimesOutOnMemberFetchThenReturnException() {
        given(clientProperties.getGroupsTimeoutMilliseconds()).willReturn(1L);

        final var mockResponse = new MockResponse()
            .setSocketPolicy(SocketPolicy.NO_RESPONSE);

        mockWebServer.enqueue(mockResponse);

        final Flux<Group> groups = groupServiceClient.getGroups();

        StepVerifier.create(groups)
            .expectError(GroupServiceUnavailableException.class)
            .verify(Duration.ofSeconds(1));
    }

}
