package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.UserRepository;
import peerport.backend.model.UserModel;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    // Create user
    public UserModel createUser(UserModel user) {
        return userRepository.save(user);
    }

    // Get all users
    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID
    public java.util.Optional<UserModel> getUserById(String uuid) {
        return userRepository.findById(uuid);
    }

    // Delete user by ID
    public boolean deleteUser(String uuid) {
        if (userRepository.existsById(uuid)) {
            userRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    // Update user
    public Optional<UserModel> updateUser(String uuid, UserModel updatedUser) {
        return userRepository.findById(uuid).map(user -> {
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            
            // Update other fields as necessary
            return userRepository.save(user);
        });
    }
}
