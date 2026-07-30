package com.weddingraffle.rifa.security;

import com.weddingraffle.rifa.config.AppProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    public JwtService(JwtEncoder jwtEncoder, AppProperties appProperties) {
        this.jwtEncoder = jwtEncoder;
        this.appProperties = appProperties;
    }

    public String generateToken(Authentication authentication) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(appProperties.jwt().expirationSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(appProperties.jwt().issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("roles", List.of(ADMIN_ROLE))
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return appProperties.jwt().expirationSeconds();
    }
}
