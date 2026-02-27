package com.coiflow.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.rs256.private-key-path}")
    private Resource privateKeyResource;

    @Value("${jwt.rs256.public-key-path}")
    private Resource publicKeyResource;

    @Value("${jwt.access-token.expiration-minutes}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token.expiration-days}")
    private long refreshTokenExpirationDays;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        this.privateKey = loadPrivateKey(privateKeyResource);
        this.publicKey = loadPublicKey(publicKeyResource);
    }

    public String generateAccessToken(String userId, String email, String role, String salonId) {
        Map<String, Object> claims = Map.of(
                "role", role,
                "salonId", salonId != null ? salonId : ""
        );
        return buildToken(userId, email, claims, accessTokenExpirationMinutes * 60 * 1000);
    }

    public String generateRefreshToken(String userId, String email) {
        return buildToken(userId, email, Map.of(), refreshTokenExpirationDays * 24 * 60 * 60 * 1000);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String extractEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String extractSalonId(String token) {
        return parseToken(token).get("salonId", String.class);
    }

    private String buildToken(String userId, String email, Map<String, Object> extraClaims, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(privateKey, Jwts.SIG.RS256);

        extraClaims.forEach(builder::claim);

        return builder.compact();
    }

    private PrivateKey loadPrivateKey(Resource resource) throws Exception {
        String pem = readPem(resource);
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                  .replace("-----END PRIVATE KEY-----", "")
                  .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                  .replace("-----END RSA PRIVATE KEY-----", "")
                  .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPublicKey(Resource resource) throws Exception {
        String pem = readPem(resource);
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                  .replace("-----END PUBLIC KEY-----", "")
                  .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String readPem(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
