package peerport.backend.controllers;

import java.util.List;

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
    
    @Autowired
    private UserService userService;

    // Get all users
    @GetMapping
    public ResponseEntity<List<UserModel>> getAllUsers() {
        List<UserModel> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Get course by ID
    @GetMapping("/{uuid}")
    public ResponseEntity<UserModel> getUserById(@PathVariable String uuid) {
        return userService.getUserById(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new course
    @PostMapping
    public ResponseEntity<UserModel> createUser(@RequestBody UserModel user) {
        UserModel savedUser = userService.createUser(user);
        return ResponseEntity.status(201).body(savedUser);
    }

    // Update course
    @PostMapping("/{uuid}")
    public ResponseEntity<UserModel> updateUser(@PathVariable String uuid, @RequestBody UserModel user) {
        return userService.updateUser(uuid, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete course
    @PostMapping("/{uuid}/delete")
    public ResponseEntity<Void> deleteUser(@PathVariable String uuid) {
        boolean deleted = userService.deleteUser(uuid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
