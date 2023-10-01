package org.grouphq.groupsync.group.sync;

import lombok.RequiredArgsConstructor;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    public Flux<PublicMember> getGroupMembers(Long groupId) {
        return groupServiceClient.getGroupMembers(groupId);
    }
}
