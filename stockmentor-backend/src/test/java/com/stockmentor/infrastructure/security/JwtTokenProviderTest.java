package com.stockmentor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenProviderTest {

    private static final String SECRET = "a".repeat(32);
    private static final long EXPIRATION_SECONDS = 7200;

    private final JwtProperties properties = new JwtProperties(SECRET, EXPIRATION_SECONDS);
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    @Test
    void issuesHs256TokenWithOnlyRequiredClaims() {
        Instant issuedAt = Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(60);

        String token = provider.issue(42L, issuedAt);
        Jwt decoded = decoderFor(SECRET).decode(token);

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(decoded.getExpiresAt()).isEqualTo(issuedAt.plusSeconds(properties.expirationSeconds()));
        assertThat(decoded.getId()).isNotBlank();
        assertThat(decoded.getClaims()).containsOnlyKeys("sub", "iat", "exp", "jti");
        assertThat(provider.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void issuesDifferentJwtIdsForTokensCreatedAtSameInstant() {
        Instant issuedAt = Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(60);

        Jwt first = decoderFor(SECRET).decode(provider.issue(42L, issuedAt));
        Jwt second = decoderFor(SECRET).decode(provider.issue(42L, issuedAt));

        assertThat(first.getId()).isNotBlank();
        assertThat(second.getId()).isNotBlank().isNotEqualTo(first.getId());
    }

    @Test
    void exposesConfiguredExpirationSeconds() {
        assertThat(provider.expirationSeconds()).isEqualTo(EXPIRATION_SECONDS);
    }

    @Test
    void mapsInvalidSignatureToInvalidToken() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = encode("b".repeat(32), "42", issuedAt, issuedAt.plusSeconds(EXPIRATION_SECONDS));

        assertBusinessError(() -> provider.parseUserId(token), ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    void mapsExpiredTokenToTokenExpired() {
        Instant issuedAt = Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(properties.expirationSeconds() + 120);
        String token = provider.issue(42L, issuedAt);

        assertBusinessError(() -> provider.parseUserId(token), ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @Test
    void mapsMalformedTokenToInvalidToken() {
        assertBusinessError(() -> provider.parseUserId("not-a-jwt"), ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    void mapsNonNumericSubjectToInvalidToken() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = encode(SECRET, "not-a-number", issuedAt, issuedAt.plusSeconds(EXPIRATION_SECONDS));

        assertBusinessError(() -> provider.parseUserId(token), ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    void rejectsAlteredToken() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = provider.issue(42L, issuedAt);

        assertBusinessError(() -> provider.parseUserId(alterSignature(token)), ErrorCode.AUTH_INVALID_TOKEN);
    }

    private JwtDecoder decoderFor(String secret) {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private String encode(String secret, String subject, Instant issuedAt, Instant expiresAt) {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String alterSignature(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char originalCharacter = token.charAt(signatureStart);
        char alteredCharacter = originalCharacter == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + alteredCharacter + token.substring(signatureStart + 1);
    }

    private void assertBusinessError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
                                     ErrorCode expectedErrorCode) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}
