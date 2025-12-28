package peerport.backend.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import peerport.backend.database.UserRepository;
import peerport.backend.dto.UserDTO;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

@Service
public class AuthService extends DefaultOAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;

    // Load the user into the daatabase upon authentication
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extrat user info from OAuth2 provider
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        final String providerId;
        Object sub = oauth2User.getAttribute("sub");
        Object id = oauth2User.getAttribute("id");
        
        if (sub != null) {
            providerId = sub.toString();
        } else if (id != null) {
            providerId = id.toString();
        } else {
            providerId = null;
        }

        // GITHUB specific handling
        final String profilePictureUrl;
        if (provider.equals("github")) {
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            profilePictureUrl = oauth2User.getAttribute("avatar_url");
        } else {
            profilePictureUrl = oauth2User.getAttribute("picture");
        }

        // Save or update user in the database
        UserModel user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> {
                UserModel newUser = new UserModel();
                newUser.setProvider(provider);
                newUser.setProviderId(providerId);
                return newUser;
            });
        
        user.setEmail(email);
        user.setName(name);
        user.setProfilePictureUrl(profilePictureUrl);

        // Default role is STUDENT
        if (user.getRole() == null) {
            user.setRole(Role.ADMIN); // Change to Role.STUDENT in production
        }
        userRepository.save(user);

        return oauth2User;
    }

    public boolean hasRole(Role role) {
        Optional<UserModel> user = currentUser();
        return user.isPresent() &&
            user.get().getRole() != null &&
            user.get().getRole().equals(role);
    }

    public boolean hasAnyRole(Role... roles) {
        Set<Role> allowed = new HashSet<>(Arrays.asList(roles));
        Optional<UserModel> user = currentUser();
        return user.isPresent() &&
            user.get().getRole() != null &&
            allowed.contains(user.get().getRole());
    }

    public Optional<UserDTO> getUser() {
        Optional<UserModel> user = currentUser();
        if (user.isPresent()) {
            return Optional.of(user.get().toDTO());
        } else {
            return Optional.empty();
        }
    }

    public Optional<UserModel> getCurrentUser() {
        return currentUser();
    }

    // Get the currently authenticated user
    private Optional<UserModel> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Check if the user is authenticated and is an OAuth2User
        if (!(auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof OAuth2User oauth)) {
            return Optional.empty();
        }

        // Check by email
        String email = oauth.getAttribute("email");
        if (email != null) {
            return userRepository.findByEmail(email);
        }

        // Check by provider and provider ID
        String providerId = attr(oauth, "sub", "id");
        String provider = auth instanceof OAuth2AuthenticationToken t ? t.getAuthorizedClientRegistrationId() : null;
        if (provider != null && providerId != null) {
            return userRepository.findByProviderAndProviderId(provider, providerId);
        }
        return Optional.empty();
    }

    // Helper method to get attribute with multiple possible keys
    private String attr(OAuth2User oauth, String... keys) {
        for (String k : keys) {
            Object v = oauth.getAttribute(k);
            if (v != null) return v.toString();
        }
        return null;
    }
}
