package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.model.UserModel;
import peerport.backend.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {
    protected static final Logger logger = LogManager.getLogger();
    
    @Autowired
    private UserService userService;

    // Get all users
    @GetMapping
    public ResponseEntity<List<UserModel>> getAllUsers() {
        logger.debug("Retrieving all users");

        List<UserModel> users = userService.getAllUsers();

        logger.debug("Successfully retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    // Get course by ID
    @GetMapping("/{uuid}")
    public ResponseEntity<UserModel> getUserById(@PathVariable String uuid) {
        logger.debug("Attempting to retrieve user with ID: {}", uuid);

        Optional<UserModel> userOpt = userService.getUserById(uuid);
        if (userOpt.isPresent()) {
            logger.debug("Successfully retrieved user with ID: {}", uuid);
            return ResponseEntity.ok(userOpt.get());
        }

        logger.info("User with ID: {} not found", uuid);
        return ResponseEntity.notFound().build();
    }

    // Create new course
    @PostMapping
    public ResponseEntity<UserModel> createUser(@RequestBody UserModel user) {
        logger.debug("Creating a new user");

        UserModel savedUser = userService.createUser(user);

        logger.debug("Successfully created user with ID: {}", savedUser.getUserId());
        return ResponseEntity.status(201).body(savedUser);
    }

    // Update course
    @PostMapping("/{uuid}")
    public ResponseEntity<UserModel> updateUser(@PathVariable String uuid, @RequestBody UserModel user) {
        logger.debug("Attempting to update user with ID: {}", uuid);

        Optional<UserModel> updatedUserOpt = userService.updateUser(uuid, user);
        if (updatedUserOpt.isPresent()) {
            logger.debug("Successfully updated user with ID: {}", uuid);
            return ResponseEntity.ok(updatedUserOpt.get());
        }

        logger.info("User with ID: {} not found, cannot update", uuid);
        return ResponseEntity.notFound().build();
    }

    // Delete course
    @PostMapping("/{uuid}/delete")
    public ResponseEntity<Void> deleteUser(@PathVariable String uuid) {
        logger.debug("Deleting user with ID: {}", uuid);

        boolean deleted = userService.deleteUser(uuid);
        if (deleted) {
            logger.debug("Successfully deleted user with ID: {}", uuid);
            return ResponseEntity.noContent().build();
        }

        logger.info("User with ID: {} not found, cannot delete", uuid);
        return ResponseEntity.notFound().build();
    }
}
