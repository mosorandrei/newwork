package com.newwork.core.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EID  = "eid";

    private final Algorithm alg;
    private final String issuer;
    private final long expirySeconds;

    public JwtUtil(
            @Value("${app.auth.hmacSecret:devsecret}") String secret,
            @Value("${app.auth.issuer:newwork}") String issuer,
            @Value("${app.auth.expirySeconds:28800}") long expirySeconds // 8h
    ) {
        this.alg = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
    }

    public String sign(UUID userId, Role role, UUID employeeId) {
        Instant now = Instant.now();
        var builder = JWT.create()
                .withIssuer(issuer)
                .withSubject(userId.toString())
                .withClaim(CLAIM_ROLE, role.name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expirySeconds)));
        if (employeeId != null) {
            builder.withClaim(CLAIM_EID, employeeId.toString());
        }
        return builder.sign(alg);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(alg)
                .withIssuer(issuer)
                .build()
                .verify(token);
    }

    public UserPrincipal toPrincipal(DecodedJWT jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String roleStr = jwt.getClaim(CLAIM_ROLE).asString();
        String eidStr  = jwt.getClaim(CLAIM_EID).asString();
        UUID employeeId = eidStr != null ? UUID.fromString(eidStr) : null;
        Role role = Role.valueOf(roleStr);
        return new UserPrincipal(userId, role, employeeId);
    }
}
