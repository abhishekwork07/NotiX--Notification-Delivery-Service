package com.abhishek.notix.api_service.v2.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TenantRequestContextHolder {

    private static final ThreadLocal<AuthenticatedActor> HOLDER = new ThreadLocal<>();

    private TenantRequestContextHolder() {
    }

    public static void set(AuthenticatedActor actor) {
        HOLDER.set(actor);
    }

    public static AuthenticatedActor get() {
        return HOLDER.get();
    }

    public static AuthenticatedActor require() {
        AuthenticatedActor actor = HOLDER.get();
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant authentication is required");
        }
        return actor;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
