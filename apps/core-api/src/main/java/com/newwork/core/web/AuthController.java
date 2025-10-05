package com.newwork.core.web;

import com.newwork.core.security.JwtUtil;
import com.newwork.core.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequestMapping("/auth")
public class AuthController {
    private record LoginReq(@Email String email, @NotBlank String password) {}

    private final UserService userService; private final JwtUtil jwt;
    public AuthController(UserService userService, JwtUtil jwt){ this.userService = userService; this.jwt = jwt; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        return userService.authenticate(req.email(), req.password())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(
                        Map.of("token", jwt.sign(u.getId(), u.getRole(), u.getEmployeeId()),
                                "role", u.getRole().name(), "employeeId", u.getEmployeeId())))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error","Invalid credentials")));
    }
}
