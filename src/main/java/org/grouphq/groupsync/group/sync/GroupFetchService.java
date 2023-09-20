package org.grouphq.groupsync.group.sync;

import org.grouphq.groupsync.groupservice.GroupServiceClient;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class GroupFetchService {

    private final GroupServiceClient groupServiceClient;

    public GroupFetchService(GroupServiceClient groupServiceClient) {
        this.groupServiceClient = groupServiceClient;
    }

    public Flux<Group> getGroups() {
        return groupServiceClient.getGroups();
    }
}
