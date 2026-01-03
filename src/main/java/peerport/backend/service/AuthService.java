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

import peerport.backend.database.UsersRepository;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotFoundException;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

/**
 * Service for handling authentication and user-related operations
 */
@Service
public class AuthService extends DefaultOAuth2UserService {

    public static final Role STUDENT = Role.STUDENT;
    public static final Role INSTRUCTOR = Role.INSTRUCTOR;
    public static final Role ADMIN = Role.ADMIN;
    
    @Autowired
    private UsersRepository userRepository;


    // Load the user into the database upon authentication
    /**
     * Loads the OAuth2 user and saves/updates them in the database
     * @param userRequest - The OAuth2 user request
     * @return The OAuth2User
     * @throws OAuth2AuthenticationException If there was an error during authentication
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract user info from OAuth2 provider
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


    // User Authorization Methods //
    /**
     * Checks if the current user has the specified role
     * 
     * @param role - The role to check
     * @return True if the user has the role, false otherwise
     */
    public boolean hasRole(Role role) {
        UserModel user = getCurrentUser();
        return user.getRole() != null &&
            user.getRole().equals(role);
    }

    /**
     * Checks if the current user has any of the specified roles
     * 
     * @param roles - The roles to check
     * @return True if the user has any of the roles, false otherwise
     */
    public boolean hasAnyRole(Role... roles) {
        Set<Role> allowed = new HashSet<>(Arrays.asList(roles));
        UserModel user = getCurrentUser();
        return user.getRole() != null &&
            allowed.contains(user.getRole());
    }


    // User Retrieval Methods //
    /**
     * Gets the current authenticated user as a UserModel
     * 
     * @return The UserModel of the current user
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found in the database (Handled in GlobalExceptionHandler)
     */
    public UserModel getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Check if the user is authenticated and is an OAuth2User
        if (!(auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof OAuth2User oauth)) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        // Check by email
        String email = oauth.getAttribute("email");
        if (email != null) {
            Optional<UserModel> user = userRepository.findByEmail(email);
            if (user.isPresent()) {
                return user.get();
            }
        }

        // Check by provider and provider ID
        String providerId = attr(oauth, "sub", "id");
        String provider = auth instanceof OAuth2AuthenticationToken t ? t.getAuthorizedClientRegistrationId() : null;
        if (provider != null && providerId != null) {
            Optional<UserModel> user = userRepository.findByProviderAndProviderId(provider, providerId);
            if (user.isPresent()) {
                return user.get();
            }
        }

        if (email != null) {
            throw new UserNotFoundException("User not found with email: " + email + " or provider: " + provider);
        } else {
            throw new UserNotFoundException("User not found with provider: " + provider);
        }
    }


    // Helpers //
    /**
     * Gets an attribute from the OAuth2User using multiple possible keys
     * 
     * @param oauth The OAuth2User object
     * @param keys Possible keys to look for the attribute
     * @return The attribute value as a String, or null if not found
     */
    private String attr(OAuth2User oauth, String... keys) {
        for (String k : keys) {
            Object v = oauth.getAttribute(k);
            if (v != null) return v.toString();
        }
        return null;
    }
}
