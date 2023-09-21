package org.grouphq.groupsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The entry point to the application setting up the Spring Context.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GroupSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupSyncApplication.class, args);
    }

}
