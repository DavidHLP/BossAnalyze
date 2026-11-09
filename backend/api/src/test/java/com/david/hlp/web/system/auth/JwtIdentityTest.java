package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.mapper.TokenMapper;
import com.david.hlp.web.system.auth.utils.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtIdentityTest {
    static final byte[] KEY = new byte[32]; // Synthetic signing material only.

    static JwtService service() {
        var service = new JwtService(mock(TokenMapper.class));
        ReflectionTestUtils.setField(service, "secretKey", Base64.getEncoder().encodeToString(KEY));
        ReflectionTestUtils.setField(service, "jwtExpiration", 3_600_000L);
        ReflectionTestUtils.setField(service, "refreshExpiration", 7_200_000L);
        return service;
    }

    static Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(KEY)).build().parseClaimsJws(token).getBody();
    }

    @Test void everyPathHasIndependentIdentityEvenWithinSameSecondAndCannotOverrideIt() {
        var service = service();
        var user = User.withUsername("synthetic@example.test").password("unused").authorities("read").build();
        var extra = new HashMap<String, Object>(Map.of("jti", "caller-chosen", "custom", "preserved"));
        Set<String> ids = new HashSet<>(), tokens = new HashSet<>();
        Map<Date, Integer> bySecond = new HashMap<>();
        for (int i = 0; i < 64; i++) {
            var issued = List.of(service.generateToken(user), service.generateToken(extra, user), service.generateRefreshToken(user));
            long[] lifetimes = {86_400_000L, 3_600_000L, 7_200_000L};
            for (int path = 0; path < issued.size(); path++) {
                String token = issued.get(path);
                var claims = claims(token);
                assertEquals(claims.getId(), UUID.fromString(claims.getId()).toString());
                assertEquals(4, UUID.fromString(claims.getId()).version());
                assertNotEquals("caller-chosen", claims.getId());
                assertTrue(ids.add(claims.getId()));
                assertTrue(tokens.add(token));
                assertEquals(user.getUsername(), claims.getSubject());
                assertEquals(lifetimes[path], claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
                assertTrue(service.isTokenValid(token, user));
                if (path == 1) assertEquals("preserved", claims.get("custom"));
                bySecond.merge(claims.getIssuedAt(), 1, Integer::sum);
            }
        }
        assertTrue(bySecond.values().stream().anyMatch(count -> count > 1), "exercise same-second issuance");
        assertEquals("caller-chosen", extra.get("jti"), "do not mutate caller claims");
    }

    @Test void legacySignedTokenWithoutJtiRemainsValid() {
        var user = User.withUsername("legacy@example.test").password("unused").authorities("read").build();
        String legacy = Jwts.builder().setSubject(user.getUsername()).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY), SignatureAlgorithm.HS256).compact();
        assertNull(claims(legacy).getId());
        assertTrue(service().isTokenValid(legacy, user));
    }
}
