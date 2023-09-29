package org.grouphq.groupsync.group.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.GroupServiceTimeoutException;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Tag("UnitTest")
class GroupServiceClientTest {
    private MockWebServer mockWebServer;
    private GroupServiceClient groupServiceClient;
    private ObjectMapper objectMapper;

    private WebClient webClient;

    private URI mockWebServerUri;

    @BeforeEach
    void setup() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        mockWebServerUri = mockWebServer.url("/").uri();
        webClient = WebClient.builder()
            .baseUrl(mockWebServerUri.toString())
            .build();

        final var clientProperties = new ClientProperties(mockWebServerUri, 1000L, 1000L);
        this.groupServiceClient = new GroupServiceClient(webClient, clientProperties);
    }

    @AfterEach
    void clean() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("When there are active groups, then return a list of active groups")
    void whenActiveGroupsExistThenReturnActiveGroups() throws JsonProcessingException {
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
    @DisplayName("Timeout appropriately when group service does not respond with groups")
    void whenGroupServiceTimesOutOnGroupsFetchThenReturnException() {
        var clientProperties = new ClientProperties(mockWebServerUri, 1L, 1L);
        groupServiceClient = new GroupServiceClient(webClient, clientProperties);

        final var mockResponse = new MockResponse()
            .setSocketPolicy(SocketPolicy.NO_RESPONSE);

        mockWebServer.enqueue(mockResponse);

        final Flux<Group> groups = groupServiceClient.getGroups();

        StepVerifier.create(groups)
            .expectError(GroupServiceTimeoutException.class)
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("When there are group members, then return a list of group members")
    void whenGroupMembersExistThenReturnGroupMembers() throws JsonProcessingException {
        final Member[] testMembers = {
            GroupTestUtility.generateFullMemberDetails("Member A", 1L),
            GroupTestUtility.generateFullMemberDetails("Member B", 1L)
        };

        final var membersAsJson = objectMapper.writeValueAsString(testMembers);

        final var mockResponse = new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(membersAsJson);

        mockWebServer.enqueue(mockResponse);

        final Flux<Member> members = groupServiceClient.getGroupMembers(1L);

        StepVerifier.create(members)
            .expectNext(testMembers[0])
            .expectNext(testMembers[1])
            .verifyComplete();
    }

    @Test
    @DisplayName("Timeout appropriately when group service does not respond with group members")
    void whenGroupServiceTimesOutOnGroupMembersFetchThenReturnException() {
        var clientProperties = new ClientProperties(mockWebServerUri, 1L, 1L);
        groupServiceClient = new GroupServiceClient(webClient, clientProperties);

        final var mockResponse = new MockResponse()
            .setSocketPolicy(SocketPolicy.NO_RESPONSE);

        mockWebServer.enqueue(mockResponse);

        final Flux<Member> members = groupServiceClient.getGroupMembers(1L);

        StepVerifier.create(members)
            .expectError(GroupServiceTimeoutException.class)
            .verify(Duration.ofSeconds(1));
    }

}
