package com.newwork.core.service.impl;

import com.newwork.core.domain.User;
import com.newwork.core.repo.UserRepository;
import com.newwork.core.service.UserService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;

    public DefaultUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> authenticate(String email, String password) {
        return userRepository.findByEmail(email).filter(u -> BCrypt.checkpw(password, u.getPasswordHash()));
    }
}
