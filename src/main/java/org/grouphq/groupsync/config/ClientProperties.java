package org.grouphq.groupsync.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the group service client.
 */
@ConfigurationProperties(prefix = "grouphq")
public record ClientProperties(
    @NotNull
    URI groupServiceUri
) {
}
