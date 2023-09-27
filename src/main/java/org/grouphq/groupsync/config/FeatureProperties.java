package org.grouphq.groupsync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defines the application's feature flags.
 */
@ConfigurationProperties(prefix = "grouphq.features.groups")
public class FeatureProperties {
    Boolean create;
    Boolean status;
    Boolean join;
    Boolean leave;
}
