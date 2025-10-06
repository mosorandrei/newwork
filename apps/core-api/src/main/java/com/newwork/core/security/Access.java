package com.newwork.core.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class Access {
    private Access(){}

    public static boolean isManager(UserPrincipal p){
        return p != null && p.role() == Role.MANAGER;
    }
    public static boolean isOwner(Object principal, UUID employeeId) {
        if (employeeId == null) return false;
        if (!(principal instanceof UserPrincipal up)) return false;
        return employeeId.equals(up.employeeId());
    }
    public static boolean canViewSensitive(UserPrincipal p, UUID employeeId){
        return isManager(p) || isOwner(p, employeeId);
    }
    public static boolean canEditProfile(UserPrincipal p, UUID employeeId){
        return isManager(p) || isOwner(p, employeeId);
    }

    public static void requireAuth(UserPrincipal p) {
        if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    public static void requireManager(UserPrincipal p) {
        requireAuth(p);
        if (p.role() != Role.MANAGER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public static void requireOwner(UserPrincipal p, UUID employeeId) {
        requireAuth(p);
        if (!isOwner(p, employeeId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public static void requireOwnerOrManager(UserPrincipal p, UUID employeeId) {
        requireAuth(p);
        if (p.role() != Role.MANAGER && !isOwner(p, employeeId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public static void requireAnyRole(UserPrincipal p, Role... roles) {
        requireAuth(p);
        for (Role r : roles) if (p.role() == r) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
