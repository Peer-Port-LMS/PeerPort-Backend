package peerport.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.dto.UserDTO;
import peerport.backend.service.AuthService;

/**
 * Exposes login endpoints that redirect clients to the configured OAuth2 providers.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Redirects to the OAuth2 provider's authorization endpoint.
     * @param provider - The OAuth2 provider (e.g., "google", "github")
     * @return A redirect response to the provider's authorization endpoint
     */
    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, "/oauth2/authorization/" + provider);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * Get the currently logged in user's information.
     * @return The UserDTO of the currently logged in user
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me() {
        // Get the logged in user's info from the security context
        // and return it as a DTO
        return ResponseEntity.ok(authService.getCurrentUser().toDTO());
    }
}
