package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import peerport.backend.controllers.UserController;
import peerport.backend.model.UserModel;
import peerport.backend.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Test
    void getAllUsers_returns200AndBody() {
        List<UserModel> users = List.of(new UserModel(), new UserModel());
        when(userService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<UserModel>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getUserById_whenFound_returns200() {
        UserModel user = new UserModel();
        when(userService.getUserById("u1")).thenReturn(Optional.of(user));

        ResponseEntity<UserModel> response = userController.getUserById("u1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }

    @Test
    void getUserById_whenMissing_returns404() {
        when(userService.getUserById("missing")).thenReturn(Optional.empty());

        ResponseEntity<UserModel> response = userController.getUserById("missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createUser_returns201() {
        UserModel user = new UserModel();
        when(userService.createUser(user)).thenReturn(user);

        ResponseEntity<UserModel> response = userController.createUser(user);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(user, response.getBody());
    }

    @Test
    void updateUser_whenFound_returns200() {
        UserModel user = new UserModel();
        when(userService.updateUser("u1", user)).thenReturn(Optional.of(user));

        ResponseEntity<UserModel> response = userController.updateUser("u1", user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }

    @Test
    void updateUser_whenMissing_returns404() {
        UserModel user = new UserModel();
        when(userService.updateUser("missing", user)).thenReturn(Optional.empty());

        ResponseEntity<UserModel> response = userController.updateUser("missing", user);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteUser_whenDeleted_returns204() {
        when(userService.deleteUser("u1")).thenReturn(true);

        ResponseEntity<Void> response = userController.deleteUser("u1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).deleteUser("u1");
    }

    @Test
    void deleteUser_whenMissing_returns404() {
        when(userService.deleteUser("missing")).thenReturn(false);

        ResponseEntity<Void> response = userController.deleteUser("missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
