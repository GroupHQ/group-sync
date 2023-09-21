package org.grouphq.groupsync.groupservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.testutility.GroupTestUtility;
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

    @BeforeEach
    void setup() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        final var webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").uri().toString())
            .build();
        this.groupServiceClient = new GroupServiceClient(webClient);
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
}
