package com.newwork.core.service;

import com.newwork.core.domain.User;

import java.util.Optional;

public interface UserService {
    Optional<User> authenticate(String email, String password);
}
