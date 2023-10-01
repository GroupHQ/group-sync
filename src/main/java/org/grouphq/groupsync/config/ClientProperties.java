package org.grouphq.groupsync.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the group service client.
 */
@ConfigurationProperties(prefix = "grouphq.group-service")
public record ClientProperties(
    @NotNull
    URI url,

    @NotNull
    Long getGroupsTimeoutMilliseconds,

    @NotNull
    Long getGroupsRetryAttempts,

    @NotNull
    Long getGroupsRetryBackoffMilliseconds,

    @NotNull
    Long getGroupMembersTimeoutMilliseconds,

    @NotNull
    Long getGroupMembersRetryAttempts,

    @NotNull
    Long getGroupMembersRetryBackoffMilliseconds
) {
}
