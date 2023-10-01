package org.grouphq.groupsync.group.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Provides REST endpoints for fetching groups and group members.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("groups")
public class GroupController {

    private final GroupFetchService groupFetchService;

    @GetMapping
    public Flux<Group> getAllGroups() {
        return groupFetchService.getGroups();
    }

    @GetMapping("/{groupId}/members")
    public Flux<PublicMember> getGroupMembers(@PathVariable Long groupId) {
        return groupFetchService.getGroupMembers(groupId);
    }
}
