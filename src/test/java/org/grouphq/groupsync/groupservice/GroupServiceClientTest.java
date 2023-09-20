package org.grouphq.groupsync.groupservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Tag("UnitTest")
public class GroupServiceClientTest {
    private MockWebServer mockWebServer;
    private GroupServiceClient groupServiceClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        var webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").uri().toString())
            .build();
        this.groupServiceClient = new GroupServiceClient(webClient);
    }

    @AfterEach
    void clean() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void whenActiveGroupsExistThenReturnActiveGroups() throws JsonProcessingException {
        Group[] testGroups = {
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE),
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE)
        };

        var groupsAsJson = objectMapper.writeValueAsString(testGroups);

        var mockResponse = new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(groupsAsJson);

        mockWebServer.enqueue(mockResponse);

        Flux<Group> groups = groupServiceClient.getGroups();

        StepVerifier.create(groups)
            .expectNext(testGroups[0])
            .expectNext(testGroups[1])
            .verifyComplete();
    }
}
