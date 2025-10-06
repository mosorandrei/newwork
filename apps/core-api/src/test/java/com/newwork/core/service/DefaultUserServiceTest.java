package com.newwork.core.service;

import com.newwork.core.domain.User;
import com.newwork.core.repo.UserRepository;
import com.newwork.core.service.impl.DefaultUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new DefaultUserService(userRepository);
    }

    private static User mkUser(String email, String plainPassword) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash(BCrypt.hashpw(plainPassword, BCrypt.gensalt())); // create a real bcrypt hash
        // optionally set role/employeeId if your domain requires
        return u;
    }

    @Test
    void authenticate_success_returnsUser() {
        String email = "manager@newwork.test";
        String pwd   = "Passw0rd!";
        User user    = mkUser(email, pwd);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = service.authenticate(email, pwd);

        assertThat(result).containsSame(user);
        verify(userRepository, times(1)).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void authenticate_wrongPassword_returnsEmpty() {
        String email = "manager@newwork.test";
        String correct = "Passw0rd!";
        String wrong   = "badpass";
        User user      = mkUser(email, correct);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = service.authenticate(email, wrong);

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void authenticate_userNotFound_returnsEmpty() {
        String email = "nobody@newwork.test";
        String pwd   = "whatever";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = service.authenticate(email, pwd);

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
    }
}
