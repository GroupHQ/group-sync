package org.grouphq.groupsync.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grouphq")
public record ClientProperties(
    @NotNull
    URI groupServiceUri
) {
}
