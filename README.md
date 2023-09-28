# group-sync
- [Client-Server Data Synchronization Discussion](#Client-Server-Data-Synchronization-Discussion)
- [Group Sync Architecture](#Group-Sync-Architecture)
  - [Component Diagram](#Component-Diagram)
- [RSocket Testing](#RSocket-Testing)
  - [Introducing the RSocket Client CLI (RSC)](#Introducing-the-RSocket-Client-CLI-RSC)
    - [Spring Boot Configuration](#Spring-Boot-Configuration)
  - [Testing the RSocket Server](#Testing-the-RSocket-Server)
    - [Initial Connection (Setup)](#Initial-Connection-Setup)
    - [Using the Connection](#Using-the-Connection)
      - [Requests a stream of updates](#Requests-a-stream-of-updates)
      - [Requests a stream of user-specific updates](#Requests-a-stream-of-user-specific-updates)
      - [Requests to join a group](#Requests-to-join-a-group)
      - [Request to leave a group](#Request-to-leave-a-group)

## Client-Server Data Synchronization Discussion
There are several ways to manage synchronization of data between a client and a server. Below are the most common strategies:
- Polling: Client polls the server periodically to check for new data.
- Long Polling: Client polls the server and holds the connection open until it receives new data or until a timeout is reached.
- Server-Side-Events (SSE): A uni-directional communication method where the server sends data to a client on a single connection.
- WebSockets: A bi-directional communication method where the client and a server share a single connection from which they can send messages to each other.

Polling and long polling strategies usually lead to an inefficient use of resources since not all polls will lead to updates. Server-Side-Events are great for use-cases where the data being sent
is read-only, and the client is not allowed or expected to alter the data since the connection doesn't allow bi-directional communication. For use-cases where the client _is_ allowed to change the data
that the server is updating the client with, then WebSockets provide a natural solution. Since it utilizes one persistent connection, it is more efficient than polling which goes through the HTTP handshake
for every poll, resulting in three trips between the client and server every time (SYN, SYN-ACK, ACK). WebSockets and SSE only go through this process on the initial connection setup.

The purpose of Group Sync is to keep clients in-sync with Group data, such as if a member joined a group, a group was created, disbanded, etc.
For the GroupHQ Demo, clients can send join or leave requests to Group Sync. To avoid the ineffecient resource usage and latency introduced by polling, we then have to 
decide between SSE and WebSockets. Since clients can change data, SSE would have to be used with an HTTP request every time a user wants to make a request.
This approach would result in low-latency updates from the server, but higher latency for making requests for changes. This would be a valid approach for scenarios where the 
client is expected to make infrequent requests, and the requests they do make are not time-sensitive.

While it may seem like Group Sync could fit this scenario well, since clients
are not expected to join or leave groups frequently, consider the following scenario:
<hr>
Scenario:
- Client A has a ping of 100ms from the nearest Group Sync server. This means it takes Client A 300ms to establish an HTTP connection over TCP with the server.
- Client B has a ping of 20ms from the nearest Group Sync server. This means it takes Client A 60ms to establish an HTTP connection over TCP with the server.
- There is one spot remaining a group that both Client A and B want to join.
<hr>
Time: 0ms
<br>Client A sends an HTTP request to join the group.
<hr>
Time: 200ms
<br>Client B sends an HTTP request to join the group.
<hr>
Time: 260ms
<br>The three roundtrips necessary to establish the TCP connection have been completed for Client B. The server then successfully processes Client B's join request.
<hr>
Time: 270ms
<br>The server successfully processes Client B's join request.
<hr>
Time: 290ms
<br>Server sends update to Client B that they have joined the group.
<hr>
Time: 300ms
<br>The three roundtrips necessary to establish the TCP connection have been completed for Client A. The server then unsuccessfully processes Client A's join request, since Client B took the last spot already.
<hr>
Time: 400ms
<br>Server sends update to Client A that they have not joined the group.
<hr>
In this scenario, Client A is unfortunate enough to not be as near to a server as Client B, resulting in Client B joining the group before Client A, even though Client A made the request first.
But would using WebSockets have made a difference anyway? Let's consider the same scenario, but now both clients have a WebSocket connection to the server, meaning that only one trip is needed
for the server to process the request:
<hr>
Time: 0ms
<br>Client A sends an HTTP request to join the group.
<hr>
Time: 100ms
<br>The server successfully processes Client A's join request.
<hr>
Time: 120ms
<br>The server sends an update to Client B (and all other connected clients) that the group is now full.
<hr>
Now, not only does Client A join before Client B, but the server had to deal with only one request instead of two (assuming Client B's browser prevents them from making a request if the group is full).
While this may seem like a contrived example, consider this: what if we had 1,000 users all of whom have different pings, and are interested in joining a group with one slot left? This could lead to multiple
failed requests and frustrated users. Indeed, most users of systems that have a limited quantity of X, where X is some limited resource, know this frustration all too well. While a WebSocket approach cannot 
fully solve this issue, it can help mitigate it to some degree, especially if there are many clients and a lack of servers globally. 

<br>The above was an interesting study on how higher-latency times can affect the user experience. But there is another issue using SSE with HTTP requests:
the added complexity of having to manage both types of connections in a server, whereas both connections can be consolidated into one using WebSockets.
Whether this is actually an issue is debatable, since some could argue that implementing WebSockets adds more complexity then separating the server-to-client
and client-to-server logic to SSE and HTTP requests.


For Group Sync, user experience and scalability needs are paramount, since GroupHQ is meant to be based on a Cloud Native Microservices architecture that makes efficient use of the resources provided to it to 
provide the fastest response times to users with low-operating costs and maintenance complexity. Therefore, we have chosen to go with the WebSocket approach for managing user connections.


## Group Sync Architecture
The following container diagram shows Group Sync's place in the GroupHQ Software System. Shown in the diagram, Group Sync communicates with three downstream services (Group Service, an Event Broker, and Config Service),
while being called by an upstream service (Edge Service, i.e. an API Gateway).
![structurizr-1-GroupHQ_Demo_Containers](https://github.com/GroupHQ/group-sync/assets/88041024/36f57630-ecda-4ad3-bee1-6731a9e11bba)

<hr>

### Component Diagram

![structurizr-1-GroupHQ_GroupSyncService_Components](https://github.com/GroupHQ/group-sync/assets/88041024/90dbc507-f29d-49b3-b5bb-94fe1ab74bb0)

### Example of Event Flow: User Joining a Group
#### 1. Group Sync Publishes a Group Join Request to the Event Broker
![structurizr-1-GroupHQ_GroupSyncJoinGroupPart1_Dynamic](https://github.com/GroupHQ/group-sync/assets/88041024/53e8a8e5-f913-41b7-a056-1c57ea6cc85a)
#### 2. Group Service Consumes this Request From the Broker And Publishes an OutboxEvent to the Broker
![structurizr-1-GroupHQ_GroupSyncJoinGroupPart2_Dynamic](https://github.com/GroupHQ/group-sync/assets/88041024/a190b95f-4751-4c09-a9a6-ba2ddd80b5c1)
#### 3. Group Sync Consumes this OutboxEvent and Forwards the Event Info to all Connected Users
![structurizr-1-GroupHQ_GroupSyncJoinGroupPart3_Dynamic](https://github.com/GroupHQ/group-sync/assets/88041024/3e86c40c-4d4b-4909-9aae-9b0450e1c152)


## RSocket Testing
Group Sync contains both integration and acceptance tests that use utilities provided by Spring libraries to test RSocket functionality.
While this is convenient, one can benefit from a deeper understanding on how RSocket requests are actually constructed.
For more details on RSocket and how to manually send RSocket requests (analagous to HTTP requests), refer to the following sections.

<hr>

**Note: The following sections assume you are using the Windows Command Prompt. If you are using any other terminal, beware that some examples may fail to run due to differences in how your terminal may interpret command syntax.**

<hr>

### Introducing the RSocket Client CLI (RSC)
This section requires you to be familiar with the RSocket protocol in the Spring Framework.
If you are not, refer to this section of the Spring Framework docs on RSocket:

[Spring Framework / Web on Reactive Stack / RSocket](https://docs.spring.io/spring-framework/reference/rsocket.html)

To communicate with an RSocket server, we need to a client that supports
the RSocket protocol. In a frontend client, this could be done using an 
RSocket library for the language of choice. But what if we want a lightweight
way to test an RSocket server without having to configure a client? This is
where the RSocket Client CLI (RSC) tool, comes in handy.

Described as a "curl for RSocket", RSC provides a way to communicate with an RSocket
server using the command line, just as a developer would use curl to communicate
with an HTTP server. You can view the RSC project on GitHub
[through this link.](https://github.com/making/rsc]), which provides installation
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
is hosted on **ws://localhost:9002/rsocket**. This is the URL we will use to connect to
the RSocket server using RSC.

#### Spring Security RSocket
Spring Security RSocket integrates RSocket connections with Spring Security, allowing
RSocket connections to be authenticated based on the authentication strategy specified.
For example, the following applies the [simple authentication type](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md)
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

This bean configures all RSocket connections to be authenticated using the simple
authentication type on all exchanges. The Simple Authentication Type is similar to 
HTTP Basic Authentication, but the username and password are not encoded in Base64. 
Instead, they are sent as plain text.

When we include a `ReactiveAuthenticationManager` bean in our Spring Boot application,
Spring Security RSocket will use that authentication manager to authenticate RSocket
connections. For example, provides a custom `ReactiveAuthenticationManager` bean:

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
Notice that this authentication manager returns a `UsernamePasswordAuthenticationToken`
without checking the credentials the user provided. Instead, it checks if the username provided is a universal unique identifier (UUID). 
If so, it returns an `Authentication` object which allows the user to access the routes. If not, then an error is returned and the user is 
denied access. Also notice that the password the user provides does not get used. This flow fits our use case where we want:
- All RSocket connections to access any endpoint
- Attach an authentication token to the RSocket connection, which is then attached
all subsequent requests the user makes.
- Integration with Spring Security RSocket provide other security protections (e.g.
CSRF protection, locking future routes, etc.) without having to configure them ourselves.

[Click here to read more about RSocket in Spring Security.](https://docs.spring.io/spring-security/reference/reactive/integrations/rsocket.html)

### Testing the RSocket Server

#### Initial Connection (Setup)
Now that we know how our application is configured, we need to check if it works.
Using RSC, we can send the following request to retrieve all group updates as Group Sync consumes them from the event broker. 
Note that we are sending a "setup" request, which is the first request sent to an RSocket server. This request is used to establish
the connection between us and the server.

```commandline
rsc --request --setupMetadata simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --setupMetadataMimeType message/x.rsocket.authentication.v0 --route=groups.updates.all ws://localhost:9002/rsocket
```
<details>
<summary>Detailed Explanation of Command Flags</summary>
<br>
--request: Shortcut of --im REQUEST_RESPONSE

--interactionModel, --im: InteractionModel (default: REQUEST_RESPONSE)

--setupMetadata, --sm: Metadata for Setup payload

--setupMetadataMimeType, --smmt: Metadata MimeType for Setup payload (default: application/json)

--route, --r: Enable Routing Metadata Extension
</details>

Your terminal should then be in a consistent RSocket connection, receiving events as they come in.
It should periodically send pings to the Group Sync server to keep its connection alive.
If you start up the Group Service application with the Group Demo Loader feature enabled, then
you should receive three Group Created events in your terminal.

It's important to specify the type of metadata we are sending in the `setupMetadataMimeType`
argument. The type we're sending is `message/x.rsocket.authentication.v0`. This is the
type used for the simple authentication type.

For testing purposes, it's better to include the `--debug` flag to see the full request
along with the --stacktrace flag to see the full stack trace if an error occurs:

```commandline
rsc --request --sm simple:user:password --smmt message/x.rsocket.authentication.v0 --r=connect --stacktrace --debug ws://localhost:9002/rsocket
```
Notice that we use the shorthands for the flags where applicable.

#### Using the Connection
You can use the following commands to send RSocket requests to Group Sync. Note that the RSocket Client is stateless; each request results in an independent RSocket connection.

##### Requests a stream of updates
```commandline
rsc --stream --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.updates.all --stacktrace --debug ws://localhost:9002/rsocket
```
##### Requests a stream of user-specific updates
```commandline
rsc --stream --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.updates.user --stacktrace --debug ws://localhost:9002/rsocket
```
##### Requests to join a group
```commandline
rsc --fnf --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.join --data "{ \"eventId\":\"0da7c964-beec-456b-b73a-0b62f1c8699b\", \"aggregateId\":189, \"websocketId\":\"user\", \"createdDate\":\"2023-09-23T19:31:35.086587900Z\", \"username\":\"Cherry\" }" ws://localhost:9002/rsocket
```
##### Request to leave a group
```commandline
rsc --fnf --sm simple:f315fbb2-028b-4784-8ce5-cc5e4f4c672b:password --smmt message/x.rsocket.authentication.v0 --r=groups.leave --data "{ \"eventId\":\"0da7c964-beec-456b-b73a-0b62f1c8691b\", \"aggregateId\":184, \"websocketId\":\"user\", \"createdDate\":\"2023-09-23T19:31:35.086587900Z\", \"memberId\": 5 }" ws://localhost:9002/rsocket
```
