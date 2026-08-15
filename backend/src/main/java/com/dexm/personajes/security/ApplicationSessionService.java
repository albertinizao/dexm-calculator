package com.dexm.personajes.security;

import com.dexm.personajes.adapter.out.persistence.CampaignInvitationRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationSessionService {
    static final String COOKIE_NAME = "DEXM_APP_SESSION";
    private final SecurityIdentityService identities;
    private final CampaignInvitationRepository invitations;
    private final AppAuthProperties properties;

    public ApplicationSessionService(SecurityIdentityService identities, CampaignInvitationRepository invitations, AppAuthProperties properties) {
        this.identities = identities; this.invitations = invitations; this.properties = properties;
    }

    public void issue(Authentication authentication, HttpServletResponse response) {
        var identity = identities.current(authentication);
        Set<String> campaigns = identities.isAdmin(identity) ? Set.of() : invitations.findByEmailAndRevokedAtIsNull(identity.email()).stream()
                .map(i -> i.getCampaignId()).collect(Collectors.toUnmodifiableSet());
        if (!identities.isAdmin(identity) && campaigns.isEmpty()) throw new org.springframework.security.access.AccessDeniedException("No active campaign invitation found");
        String payload = identity.subject() + "|" + identities.isAdmin(identity) + "|" + String.join(",", campaigns);
        String token = encode(payload) + "." + sign(payload);
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setHttpOnly(true); cookie.setSecure(properties.mode() == AppAuthMode.IAP); cookie.setPath("/");
        response.addCookie(cookie);
    }

    public boolean isValid(HttpServletRequest request, Authentication authentication) {
        String token = cookie(request);
        if (token == null) return false;
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) return false;
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            return constantTime(sign(payload), parts[1]) && payload.startsWith(identities.current(authentication).subject() + "|");
        } catch (IllegalArgumentException error) { return false; }
    }

    public boolean hasCampaign(HttpServletRequest request, Authentication authentication, String campaignId) {
        String token = cookie(request);
        if (token == null || !isValid(request, authentication)) return false;
        String[] parts = new String(Base64.getUrlDecoder().decode(token.split("\\.", 2)[0]), StandardCharsets.UTF_8).split("\\|", 3);
        if (Boolean.parseBoolean(parts[1])) return true;
        return parts.length == 3 && Set.of(parts[2].split(",")).contains(campaignId);
    }

    public Set<String> campaignIds(HttpServletRequest request, Authentication authentication) {
        String token = cookie(request);
        if (token == null || !isValid(request, authentication)) return Set.of();
        String[] parts = new String(Base64.getUrlDecoder().decode(token.split("\\.", 2)[0]), StandardCharsets.UTF_8).split("\\|", 3);
        if (parts.length < 3 || Boolean.parseBoolean(parts[1]) || parts[2].isBlank()) return Set.of();
        return Set.of(parts[2].split(","));
    }

    public void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, ""); cookie.setHttpOnly(true); cookie.setSecure(properties.mode() == AppAuthMode.IAP); cookie.setPath("/"); cookie.setMaxAge(0); response.addCookie(cookie);
    }

    private String cookie(HttpServletRequest request) { if (request.getCookies() == null) return null; for (Cookie cookie : request.getCookies()) if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue(); return null; }
    private String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private String sign(String value) { try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(properties.getSessionSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception error) { throw new IllegalStateException(error); } }
    private boolean constantTime(String expected, String actual) { return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)); }
}
