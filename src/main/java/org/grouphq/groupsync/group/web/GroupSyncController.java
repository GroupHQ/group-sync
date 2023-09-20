package org.grouphq.groupsync.group.web;

import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("groups")
public class GroupSyncController {

    private final GroupFetchService groupFetchService;

    public GroupSyncController(GroupFetchService groupFetchService) {
        this.groupFetchService = groupFetchService;
    }

    @GetMapping
    public Flux<Group> getAllGroups() {
        return groupFetchService.getGroups();
    }
}
