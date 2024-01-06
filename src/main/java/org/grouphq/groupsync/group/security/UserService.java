package org.grouphq.groupsync.group.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for retrieving user-related information.
 */
@Service
public class UserService {
    public Mono<Authentication> getUserAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(authentication -> authentication == null
                ? Mono.empty()
                : Mono.just(authentication));
    }
}
