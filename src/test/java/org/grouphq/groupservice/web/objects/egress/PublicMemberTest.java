package org.grouphq.groupservice.web.objects.egress;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Instant;
import org.grouphq.groupsync.groupservice.domain.members.MemberStatus;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PublicMemberTest {

    @Test
    @DisplayName("Public member equality should depend only on member id")
    void publicMemberEqualityShouldDependOnlyOnMemberId() {
        final PublicMember publicMember1 = PublicMember.builder()
            .id(1L)
            .username("user1")
            .groupId(1L)
            .memberStatus(MemberStatus.KICKED)
            .joinedDate(Instant.now().toString())
            .exitedDate(Instant.now().toString())
            .build();

        final PublicMember publicMember2 = PublicMember.builder()
            .id(1L)
            .username("user2")
            .groupId(2L)
            .memberStatus(MemberStatus.ACTIVE)
            .joinedDate(Instant.now().toString())
            .exitedDate(Instant.now().toString())
            .build();

        assertThat(publicMember1).isEqualTo(publicMember2);
    }
}
