package org.grouphq.groupsync.groupservice.domain.outbox;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;

/**
 * Marker interface for objects sent in events.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Group.class, name = "Group"),
    @JsonSubTypes.Type(value = Member.class, name = "Member"),
    @JsonSubTypes.Type(value = PublicMember.class, name = "PublicMember"),
    @JsonSubTypes.Type(value = ErrorData.class, name = "ErrorData")
})
public interface EventDataModel {
}
