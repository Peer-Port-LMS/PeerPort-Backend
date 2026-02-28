package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import peerport.backend.controllers.AuthController;
import peerport.backend.dto.UserDTO;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.service.AuthService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Test
    void login_returns302WithProviderRedirect() {
        ResponseEntity<Void> response = authController.login("github");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("/oauth2/authorization/github", response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    void me_returnsCurrentUserDto() {
        UserModel user = new UserModel("u1", "Test User", "user@test.com", null, null, Role.STUDENT);
        UserDTO dto = user.toDTO();
        when(authService.getCurrentUser()).thenReturn(user);

        ResponseEntity<UserDTO> response = authController.me();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto.email, response.getBody().email);
        assertEquals(dto.name, response.getBody().name);
    }
}
