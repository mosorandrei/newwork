package com.newwork.core.web;

import com.newwork.core.security.JwtUtil;
import com.newwork.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Tag(name = "Auth")
@RestController @RequestMapping("/auth")
public class AuthController {
    private record LoginReq(
            @Schema(example = "manager@newwork.test") String email,
            @Schema(example = "Passw0rd!") String password) {}

    private final UserService userService; private final JwtUtil jwt;
    public AuthController(UserService userService, JwtUtil jwt){ this.userService = userService; this.jwt = jwt; }

    @Operation(
            summary = "Login and get JWT",
            description = "Returns a JWT token to be used in the Authorization header as **Bearer &lt;token&gt;**."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        return userService.authenticate(req.email(), req.password())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(
                        Map.of("token", jwt.sign(u.getId(), u.getRole(), u.getEmployeeId()),
                                "role", u.getRole().name(), "employeeId", u.getEmployeeId())))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error","Invalid credentials")));
    }
}
