package com.newwork.core.security;

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
}
