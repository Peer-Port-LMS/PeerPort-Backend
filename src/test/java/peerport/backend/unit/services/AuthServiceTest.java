package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import peerport.backend.database.UsersRepository;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotFoundException;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.UserModel;
import peerport.backend.service.AuthService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @InjectMocks
    @Spy
    private AuthService authService;

    @Mock
    private UsersRepository usersRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class RoleChecks {
        @Test
        void hasRole_returnsTrueWhenRoleMatches() {
            UserModel user = new UserModel("u1", "User", "u@test.com", null, null, Role.ADMIN);
            doReturn(user).when(authService).getCurrentUser();

            assertTrue(authService.hasRole(Role.ADMIN));
            assertFalse(authService.hasRole(Role.STUDENT));
        }

        @Test
        void hasAnyRole_returnsTrueWhenIncluded() {
            UserModel user = new UserModel("u1", "User", "u@test.com", null, null, Role.INSTRUCTOR);
            doReturn(user).when(authService).getCurrentUser();

            assertTrue(authService.hasAnyRole(Role.ADMIN, Role.INSTRUCTOR));
            assertFalse(authService.hasAnyRole(Role.STUDENT));
        }
    }

    @Nested
    class CurrentUserResolution {
        @Test
        void getCurrentUser_withUnauthenticatedPrincipal_throws() {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("plain", null));

            assertThrows(UserNotAuthenticatedException.class, () -> authService.getCurrentUser());
        }

        @Test
        void getCurrentUser_resolvesByEmail() {
            UserModel user = new UserModel("u1", "User", "u@test.com", null, null, Role.STUDENT);
            OAuth2AuthenticationToken authentication = oauthAuthToken("github", "u@test.com", null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            when(usersRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

            UserModel result = authService.getCurrentUser();

            assertEquals("u1", result.getUserId());
        }

        @Test
        void getCurrentUser_fallsBackToProviderLookup() {
            UserModel user = new UserModel("u2", "User2", "other@test.com", null, null, Role.STUDENT);
            OAuth2AuthenticationToken authentication = oauthAuthToken("github", "missing@test.com", "provider-sub", null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            when(usersRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
            when(usersRepository.findByProviderAndProviderId("github", "provider-sub")).thenReturn(Optional.of(user));

            UserModel result = authService.getCurrentUser();

            assertEquals("u2", result.getUserId());
        }

        @Test
        void getCurrentUser_whenNoMatch_throwsUserNotFound() {
            OAuth2AuthenticationToken authentication = oauthAuthToken("github", "missing@test.com", "provider-sub", null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            when(usersRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
            when(usersRepository.findByProviderAndProviderId("github", "provider-sub")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authService.getCurrentUser());
        }
    }

    private OAuth2AuthenticationToken oauthAuthToken(String provider, String email, String sub, String id) {
        Map<String, Object> attributes = new HashMap<>();
        if (email != null) {
            attributes.put("email", email);
        }
        if (sub != null) {
            attributes.put("sub", sub);
        }
        if (id != null) {
            attributes.put("id", id);
        }

        OAuth2User principal = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );

        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), provider);
    }
}
