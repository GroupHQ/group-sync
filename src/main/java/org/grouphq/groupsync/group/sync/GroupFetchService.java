package org.grouphq.groupsync.group.sync;

import lombok.RequiredArgsConstructor;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Fetches groups and group members from the group service.
 */
@RequiredArgsConstructor
@Service
public class GroupFetchService {

    private final GroupServiceClient groupServiceClient;

    public Flux<Group> getGroups() {
        return groupServiceClient.getGroups();
    }

    public Mono<Member> getMyMember(String websocketId) {
        return groupServiceClient.getMyMember(websocketId);
    }
}
