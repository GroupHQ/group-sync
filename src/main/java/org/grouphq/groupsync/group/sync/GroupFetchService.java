package org.grouphq.groupsync.group.sync;

import org.grouphq.groupsync.groupservice.GroupServiceClient;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Fetches groups and group members from the group service.
 */
@Service
public class GroupFetchService {

    private final GroupServiceClient groupServiceClient;

    public GroupFetchService(GroupServiceClient groupServiceClient) {
        this.groupServiceClient = groupServiceClient;
    }

    public Flux<Group> getGroups() {
        return groupServiceClient.getGroups();
    }

    public Flux<Member> getGroupMembers(Long groupId) {
        return groupServiceClient.getGroupMembers(groupId);
    }
}
