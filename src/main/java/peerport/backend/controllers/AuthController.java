package peerport.backend.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.dto.UserDTO;
import peerport.backend.enums.ResponseCodes;
import peerport.backend.service.AuthService;

/**
 * Exposes login endpoints that redirect clients to the configured OAuth2 providers.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // Login the user via the specified OAuth2 provider
    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, "/oauth2/authorization/" + provider);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // Get the authenticated user's info
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me() {
        // Get the logged in user's info from the security context
        Optional<UserDTO> user = authService.getUser();
        
        // Check if user is exists
        if (!user.isPresent()) {
            return ResponseEntity.status(ResponseCodes.UNAUTHORIZED).build();
        }

        // Return the user
        return ResponseEntity.ok(user.get());
    }
}
