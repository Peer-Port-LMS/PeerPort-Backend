package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import peerport.backend.database.UsersRepository;
import peerport.backend.model.UserModel;
import peerport.backend.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UsersRepository usersRepository;

    @Test
    void createUser_savesAndReturnsUser() {
        UserModel user = new UserModel();
        user.setEmail("user@test.com");

        when(usersRepository.save(user)).thenReturn(user);

        UserModel result = userService.createUser(user);

        assertEquals(user, result);
        verify(usersRepository).save(user);
    }

    @Test
    void getAllUsers_returnsRepositoryData() {
        List<UserModel> users = List.of(new UserModel(), new UserModel());
        when(usersRepository.findAll()).thenReturn(users);

        List<UserModel> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals(users, result);
    }

    @Nested
    class GetByIdTests {
        @Test
        void getUserById_whenFound_returnsOptionalWithUser() {
            UserModel user = new UserModel();
            when(usersRepository.findById("u1")).thenReturn(Optional.of(user));

            Optional<UserModel> result = userService.getUserById("u1");

            assertTrue(result.isPresent());
            assertEquals(user, result.get());
        }

        @Test
        void getUserById_whenMissing_returnsEmptyOptional() {
            when(usersRepository.findById("missing")).thenReturn(Optional.empty());

            Optional<UserModel> result = userService.getUserById("missing");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void deleteUser_whenExists_deletesAndReturnsTrue() {
            when(usersRepository.existsById("u1")).thenReturn(true);

            boolean result = userService.deleteUser("u1");

            assertTrue(result);
            verify(usersRepository).deleteById("u1");
        }

        @Test
        void deleteUser_whenMissing_returnsFalseWithoutDelete() {
            when(usersRepository.existsById("missing")).thenReturn(false);

            boolean result = userService.deleteUser("missing");

            assertFalse(result);
            verify(usersRepository, never()).deleteById("missing");
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void updateUser_whenFound_updatesNameAndEmail() {
            UserModel existing = new UserModel();
            existing.setName("old");
            existing.setEmail("old@test.com");

            UserModel updates = new UserModel();
            updates.setName("new");
            updates.setEmail("new@test.com");

            when(usersRepository.findById("u1")).thenReturn(Optional.of(existing));
            when(usersRepository.save(existing)).thenReturn(existing);

            Optional<UserModel> result = userService.updateUser("u1", updates);

            assertTrue(result.isPresent());
            assertEquals("new", result.get().getName());
            assertEquals("new@test.com", result.get().getEmail());
            verify(usersRepository).save(existing);
        }

        @Test
        void updateUser_whenMissing_returnsEmpty() {
            when(usersRepository.findById("missing")).thenReturn(Optional.empty());

            Optional<UserModel> result = userService.updateUser("missing", new UserModel());

            assertTrue(result.isEmpty());
            verify(usersRepository, never()).save(org.mockito.ArgumentMatchers.any(UserModel.class));
        }
    }
}
