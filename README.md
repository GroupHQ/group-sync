_This README is a work in progress. Some steps may be incomplete or missing_

# group-sync
## Contents
- [Synopsis](#Synopsis)
  - [Synchronization Flow](#Synchronization-Flow)
    - [Public and Private Events](#Public-and-Private-Events)
    - [Join and Leave Requests](#Join-and-Leave-Requests)
    - [Retrieving Current Member Info](#Retrieving-Current-Member-Info)
- [Setting up the Development Environment](#Setting-up-the-Development-Environment)
  - [Prerequisites](#Prerequisites)
- [Developing](#Developing)
  - [Checks To Pass](#Checks-To-Pass)
    - [Code Style](#Code-Style)
    - [Code Quality](#Code-Quality)
    - [Dependency Vulnerability Check](#Dependency-Vulnerability-Check)
    - [Unit Tests](#Unit-Tests)
    - [Integration Tests](#Integration-Tests)
    - [Manifest Validation](#Manifest-Validation)
  - [User Automated Tests & Regression Testing](#User-Automated-Tests--Regression-Testing)
- [RSocket Testing](#RSocket-Testing)
  - [Introducing the RSocket Client CLI (RSC)](#Introducing-the-RSocket-Client-CLI-RSC)
    - [Spring Boot Configuration](#Spring-Boot-Configuration)
  - [Testing the RSocket Server](#Testing-the-RSocket-Server)
    - [Initial Connection (Setup)](#Initial-Connection-Setup)
    - [Types of Requests](#Types-of-Requests)
      - [Requests a stream of updates](#Requests-a-stream-of-updates)
      - [Requests a stream of user-specific updates](#Requests-a-stream-of-user-specific-updates)
      - [Requests to join a group](#Requests-to-join-a-group)
      - [Request to leave a group](#Request-to-leave-a-group)
      - [Request to get user's current active member](#Request-to-get-users-current-active-member)
- [Group Sync Architecture](#Group-Sync-Architecture)
  - [Component Diagram](#Component-Diagram)
  - [Example of Event Flow: User Joining a Group](#Example-of-Event-Flow-User-Joining-a-Group)
    - [1. Group Sync Publishes a Group Join Request to the Event Broker](#1-Group-Sync-Publishes-a-Group-Join-Request-to-the-Event-Broker)
    - [2. Group Service Consumes this Request From the Broker And Publishes an OutboxEvent to the Broker](#2-Group-Service-Consumes-this-Request-From-the-Broker-And-Publishes-an-OutboxEvent-to-the-Broker)
    - [3. Group Sync Consumes this OutboxEvent and Forwards the Event Info to all Connected Users](#3-Group-Sync-Consumes-this-OutboxEvent-and-Forwards-the-Event-Info-to-all-Connected-Users)

## Synopsis
Group Sync manages RSocket connections for users to keep in-sync with the latest group changes using Spring Security 
RSocket. The service acts as a mediator between the user and Group Service. Any requests for groups or a user’s current 
member takes place through Group Sync, which forwards the request to the Group Service REST API.

### Synchronization Flow
The main purpose of Group Sync is to forward events published by Group Service, as well as to 
publish its own events to an event broker. Once a user establishes an RSocket connection to Group Sync, 
the service sends the user a stream of public and private events via the request-stream model of the RSocket protocol. 

#### Public and Private Events
Public events involve any successful action that occurs and should be shown to users, such as a group being created
or a member joining a group. Public events have any sensitive information stripped off, such as the user ID of the user
who made the request. On the other hand, private events are sent to the user who initiated the request, whether 
successful or unsuccessful. These events include more sensitive data, such as the member ID of the member
created for a user when they join the group, allowing them to know which member is theirs and in which group.

#### Join and Leave Requests
Along with streams of events, Group Sync currently allows users to send requests to join or leave groups, as well as 
to retrieve a user’s current member. Join and leave requests use the fire-and-forget request model, where the client 
should expect no immediate response. Instead, a response is sent through their private update stream once the request 
succeeds or fails. 

#### Retrieving Current Member Info
For retrieving a user’s current member, the request-response model is used. The client uses their RSocket connection
to authenticate their request for their current member. A response is returned with the user's member info if they
have one. This information includes the current group a user's member is in.

## Setting up the Development Environment

### Prerequisites
- Recommended Java 21. Minimum Java 17. [Download here](https://www.oracle.com/java/technologies/downloads/)
- An IDE that supports Java. [IntelliJ](https://www.jetbrains.com/idea/) is recommended. A free community edition is
  available.
- Git. [Download here](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git). For Windows users, install
  Git using [Git For Windows](https://gitforwindows.org/)
- Recommended to install a Bash terminal. Linux and Mac users should have this by default. Windows users can install
  Git Bash using [Git For Windows](https://gitforwindows.org/).
- Kubeconform (for validating Kubernetes manifests). [Download here](https://github.com/yannh/kubeconform?tab=readme-ov-file#Installation)

Group Sync uses a RabbitMQ event broker. While you can download, configure, and run
this service manually, it is highly recommended to use the provided docker-compose file to run it instead,
located in the GroupHQ Deployment repository. See the [GroupHQ Deployment README](https://github.com/GroupHQ/groupHQ-deployment?tab=readme-ov-file#local-environment-using-docker)
for more information.

Once you have your backing services ready, you should be able to run the Group Sync application, either
through your IDE or through the docker-compose file in the GroupHQ Deployment repository.

Alternatively, you can run the Group Sync application in a Kubernetes environment. See the
[GroupHQ Deployment README](https://github.com/GroupHQ/groupHQ-deployment?tab=readme-ov-file#local-environment-using-kubernetes)
for instructions on setting that up.

## Developing
When developing new features, it's recommended to follow a test-driven development approach using the classicist style
of testing. What this means is:
1. Prioritize writing tests first over code. At the very minimum, write out test cases for the changes you want to
   make. This will help you think through the design of your changes, and will help you catch defects early on.
2. Avoid excessive mocking. Mocks are useful for isolating your code from external dependencies, but they can also
   make your tests brittle and hard to maintain. If you find yourself mocking a lot, it may be a sign that the class
   under test is more suitable for integration testing. If you are mocking out an external service, consider using
   a Testcontainer for simulating the service as a fake type of [test double](https://martinfowler.com/bliki/TestDouble.html)*.
3. Write tests, implement, and then most importantly, refactor and review. It's easy to get caught up in
   messy code to write code that pass tests. Always take the time to review your code after implementing a feature.


*When testing the event-messaging system with an event broker, use the Spring Cloud Stream Test Binder.
All messaging with the event broker takes place through Spring Cloud Stream. Instead of testing the dependency itself,
rely on the Spring Cloud Stream Test Binder to simulate the broker. This will allow you to test the messaging system
without having to worry about the sending and receiving of messages. See the `GroupEventForwarderIntegrationTest` class
for an example of this. See the [Spring Cloud Stream Test Binder documentation](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/spring_integration_test_binder.html)
for more information on the test binder.

### Checks To Pass
When pushing a commit to any branch, the following checks are run:
- **Code Style:** All code must pass the checkstyle rules defined in the `config/checkstyle/checkstyle.xml` file.
- **Code Quality:** All code must pass the PMD rules defined in the `config/pmd/*` files.
- **Dependency Vulnerability Check:** Dependencies with vulnerabilities that meet the specified vulnerability cutoff
  must be reviewed.
- **Unit Tests:** All unit tests must pass.
- **Integration Tests:** All integration tests must pass.
- **Manifest Validation:** Any changes to Kubernetes manifests under the `k8s` folder must pass validation.

For code style, quality, and dependency vulnerability checks, you can view a detailed report on these checks once
they have completed by navigating to the build/reports directory.
You can run these checks with the following commands (These commands are compatible with the bash terminal. If you are
using a different terminal, you may need to modify the commands to work with your terminal):

#### Code Style
```bash
./gradlew checkstyleMain --stacktrace
./gradlew checkstyleTest --stacktrace
```

#### Code Quality
```bash
./gradlew pmdMain --stacktrace
./gradlew pmdTest --stacktrace
```

#### Dependency Vulnerability Check
These commands are compatible with the bash terminal. If you are using a different terminal, you may need to modify
the commands to work with your terminal.

```bash
./gradlew dependencyCheckAnalyze --stacktrace -PnvdApiKey="YOUR_NVD_API_KEY"
```
See [here](https://nvd.nist.gov/developers/request-an-api-key) for details on requesting an NVD API key.

#### Unit Tests
```bash
./gradlew testUnit
```

#### Integration Tests
```bash
./gradlew testIntegration
```

#### Manifest Validation
```bash
kustomize build k8s/base | kubeconform -strict -summary -output json
kustomize build k8s/overlays/observability | kubeconform -strict -summary -output json
```

It's recommended to add these commands to your IDE as separate run configurations for quick access.
Make sure you do not commit these run configurations to the version control system, especially
any that may contain sensitive info (such as an NVD API key for the dependency vulnerability check).

### User Automated Tests & Regression Testing
For any features that introduce a new user-facing feature, it's recommended to add automated tests for them to
the [GroupHQ Continuous Testing Test Suite](https://github.com/GroupHQ/grouphq-continuous-testing-test-suite).
For more information on how to write these tests, see the associated READEME of that repository.

When any pull request is opened, a request is sent to the [GroupHQ Continuous Testing Proxy Server](https://github.com/GroupHQ/grouphq-continuous-testing-proxy-server)
to run the test suite against the pull request. The length of a test run is expected to vary over time,
but expect it to take no more than an hour (at the time of writing, it takes about 20 minutes).
Once the test run is complete, the results will be posted to the pull request, including a link to the test results to
review if needed.

To learn how to add a new test to the test suite, or run the test suite locally, see the
[GroupHQ Continuous Testing Test Suite README](https://github.com/GroupHQ/grouphq-continuous-testing-test-suite).
It's recommended to validate at least tests relevant to your feature, as well as any new tests added.

## RSocket Testing
Group Sync contains integration tests that use utilities provided by Spring libraries to test RSocket functionality.
While this is convenient, one can benefit from a deeper understanding on how RSocket requests are actually constructed.
For more details on RSocket and how to manually send RSocket requests using the RSocket Client CLI (analogous to using 
Curl to send HTTP requests), refer to the following sections.

<hr>

**Note: The following sections assume you are using the Windows Command Prompt. If you are using any other terminal, 
beware that some examples may fail to run due to differences in how your terminal may interpret command syntax.**

<hr>

### Introducing the RSocket Client CLI (RSC)
This section requires you to be familiar with the RSocket protocol in the Spring Framework.
If you are not, refer to this section of the Spring Framework docs on RSocket:

[Spring Framework / Web on Reactive Stack / RSocket](https://docs.spring.io/spring-framework/reference/rsocket.html)

To communicate with an RSocket server, we need a client that supports the RSocket protocol. In a frontend client, this 
could be done using an RSocket library for the language of choice, but that adds needless complexity.
One simple solution to test an RSocket server without having to configure a client is to use the RSocket Client CLI 
(RSC) tool.

Described as a "curl for RSocket", RSC provides a way to communicate with an RSocket
server using the command line, just as a developer would use curl to communicate
with an HTTP server. You can view the RSC project on GitHub
[through this link](https://github.com/making/rsc), which provides installation
instructions as well as instructions on how to use the client.

#### Spring Boot Configuration
Our RSocket server is configured and managed by Spring Boot through the following
dependency: 

`org.springframework.boot:spring-boot-starter-rsocket`

This dependency provides default configuration for the RSocket server. It can also
"plug in" the RSocket server to an existing WebFlux server if the following properties match:
```yaml
spring:
  rsocket:
    server:
      mapping-path: "/rsocket"
      transport: "websocket"
```
[Click here more info on Spring Boot's interaction with RSocket.](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#messaging.rsocket)

Assuming our WebFlux server is hosted on http://localhost:9002, then our RSocket server
is hosted on **ws://localhost:9002/rsocket**. This is the URL we'll use to connect to
the RSocket server using RSC.

#### Spring Security RSocket
Spring Security RSocket integrates RSocket connections with Spring Security, allowing them to be authenticated based on
the authentication strategy specified. For example, the following applies the [simple authentication type](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md)
to RSocket connections:

```java
@Bean
PayloadSocketAcceptorInterceptor rsocketInterceptor(RSocketSecurity socketSecurity) {
    socketSecurity.authorizePayload(authorize -> authorize
        .anyExchange().authenticated())
        .simpleAuthentication(Customizer.withDefaults());

    return socketSecurity.build();
}
```

This bean configures all RSocket connections to be authenticated using the simple authentication type on all exchanges. 
The Simple Authentication Type is similar to HTTP Basic Authentication, but the username and password are not encoded in 
Base64. Instead, they are sent as [cleartext](https://www.hypr.com/security-encyclopedia/cleartext#:~:text=Cleartext%20is%20information%20that%20is,expected%20form%2C%20consumable%20and%20readable.).

When we include a `ReactiveAuthenticationManager` bean in our Spring Boot application, Spring Security RSocket will use 
that authentication manager to authenticate RSocket connections. For example, the following provides a custom 
`ReactiveAuthenticationManager` bean:

```java
@Bean
public ReactiveAuthenticationManager reactiveAuthenticationManager() {
    return authentication -> {
        final String username = authentication.getName();

        try {
            UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid username: " + username));
        }

        return Mono.just(new UsernamePasswordAuthenticationToken(
            username,
            "dummy",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));
    };
}
```
Notice that this authentication manager returns a `UsernamePasswordAuthenticationToken` without checking the password a 
user provides. Instead, it checks if the username provided can be parsed to a universal unique identifier (UUID). 
If so, it returns an `Authentication` object which allows the user to access the routes. If not, then an error is 
returned and the user is denied access. This flow fits our use case where we want:
- All RSocket connections to access any endpoint
- Authenticate the RSocket connection, which is then authenticated for all subsequent requests the user makes.
- Integration with Spring Security RSocket provides other security protections (e.g.
CSRF protection, locking future routes, etc.) without having to configure them ourselves.

[Click here to read more about RSocket in Spring Security.](https://docs.spring.io/spring-security/reference/reactive/integrations/rsocket.html)

### Testing the RSocket Server

#### Initial Connection (Setup)
Now that we know how our application is configured, we need to check if it works.
Using RSC, we can send the following request to retrieve all group updates as Group Sync consumes them from the event 
broker. Note that we are sending a "setup" request, which is the first request sent to an RSocket server. This request 
is used to establish the connection between us and the server. Also note that when using RSC, we'll need to send a
setup request for every request, since the client will not save our connection for future requests.

```commandline
rsc --stream --setupMetadata simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --setupMetadataMimeType message/x.rsocket.authentication.v0 --route=groups.updates.all ws://localhost:9002/api/rsocket
```
<details>
<summary>Detailed Explanation of Command Flags</summary>
<br>
--interactionModel, --im: InteractionModel (default: REQUEST_RESPONSE)

--request: Shortcut of --im REQUEST_RESPONSE

--fnf: Shortcut of --im FIRE_AND_FORGET                                 

--stream: Shortcut of --im REQUEST_STREAM

--setupMetadata, --sm: Metadata for Setup payload

--setupMetadataMimeType, --smmt: Metadata MimeType for Setup payload (default: application/json)

--route, --r: Enable Routing Metadata Extension
</details>

Your terminal should then be in a persistent RSocket connection, receiving events as they come in.
It will periodically send `KEEP ALIVE` messages to the Group Sync server to keep its connection active.
If you start up the Group Service application with the Group Demo Loader feature enabled, then
you should receive three `GROUP_CREATED` events in your terminal.

It's important to specify the type of metadata we are sending in the `setupMetadataMimeType`
argument. The type we're sending is `message/x.rsocket.authentication.v0`. This is the
type used for the simple authentication type.

For testing purposes, it's better to include the `--debug` flag to see the full request
along with the `--stacktrace` flag to see the full stack trace if an error occurs:

```commandline
rsc --request --sm simple:user:password --smmt message/x.rsocket.authentication.v0 --r=connect --stacktrace --debug ws://localhost:9002/api/rsocket
```
Notice that we use the shorthands for the flags where applicable.

#### Types of Requests
You can use the following commands to send RSocket requests to Group Sync. Remember that RSC is stateless; each request results in an independent RSocket connection.

##### Requests a stream of updates
```commandline
rsc --stream --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.updates.all --stacktrace --debug ws://localhost:9002/api/rsocket
```
##### Requests a stream of user-specific updates
```commandline
rsc --stream --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.updates.user --stacktrace --debug ws://localhost:9002/api/rsocket
```
##### Requests to join a group
```commandline
rsc --fnf --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.join --data "{ \"eventId\":\"0da7c964-beec-456b-b73a-0b62f1c8699b\", \"aggregateId\":169, \"websocketId\":\"fbe943cc-b3a0-4f2e-921a-2325d64b16c9\", \"createdDate\":\"2023-09-23T19:31:35.086587900Z\", \"username\":\"Klunk\" }" ws://localhost:9002/api/rsocket

```
##### Request to leave a group
```commandline
rsc --fnf --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.leave --data "{ \"eventId\":\"fa0fcf99-2aef-4f2a-8173-6e4bee623e2a\", \"aggregateId\":169, \"websocketId\":\"fbe943cc-b3a0-4f2e-921a-2325d64b16c9\", \"createdDate\":\"2023-09-23T19:31:35.086587900Z\", \"memberId\": 3569 }" ws://localhost:9002/api/rsocket
```

#### Request to get user's current active member
```commandline
rsc --request --sm simple:32290501-5681-45f1-a14d-73c29d11d6b7:password --smmt message/x.rsocket.authentication.v0 --r=groups.user.member ws://localhost:9002/api/rsocket
```

## Group Sync Architecture
The following container diagram shows Group Sync's place in the GroupHQ Software System. Shown in the diagram,
Group Sync communicates with three downstream services: Group Service and an event broker,
while being called by an upstream service, Edge Service (i.e. GroupHQ's API Gateway).

![GroupHQ_Demo_Containers_noObservability](https://github.com/GroupHQ/group-sync/assets/88041024/833937ea-59f4-4aee-a9d1-46996f2d9848)


<hr>

### Component Diagram

![structurizr-1-GroupHQ_GroupSyncService_Components](https://github.com/GroupHQ/group-sync/assets/88041024/90dbc507-f29d-49b3-b5bb-94fe1ab74bb0)

### Example of Event Flow: User Joining a Group
#### 1. Group Sync Publishes a Group Join Request to the Event Broker
![structurizr-1-GroupHQ_GroupSyncJoinGroupPart1_Dynamic](https://github.com/GroupHQ/group-sync/assets/88041024/53e8a8e5-f913-41b7-a056-1c57ea6cc85a)
#### 2. Group Service Consumes this Request From the Broker And Publishes an OutboxEvent to the Broker
![structurizr-1-GroupHQ_GroupSyncJoinGroupPart2_Dynamic](https://github.com/GroupHQ/group-sync/assets/88041024/be44b8f5-6946-441d-b749-c6f1aa70aeed)
#### 3. Group Sync Consumes this OutboxEvent and Forwards the Event Info to all Connected Users
![structurizr-1-GroupHQ_GroupSyncJoinGroupPart3_Dynamic](https://github.com/GroupHQ/group-sync/assets/88041024/3e86c40c-4d4b-4909-9aae-9b0450e1c152)
