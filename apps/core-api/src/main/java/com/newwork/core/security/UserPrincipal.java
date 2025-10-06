package com.newwork.core.security;

import java.util.UUID;
public record UserPrincipal(UUID userId, Role role, UUID employeeId) {}
