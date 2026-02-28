package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.UsersRepository;
import peerport.backend.model.UserModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
@Service
public class UserService {
    protected static final Logger logger = LogManager.getLogger();
    
    @Autowired
    private UsersRepository userRepository;

    // Create user
    public UserModel createUser(UserModel user) {
        logger.debug("Attempting to create user with email: {}", user.getEmail());
        UserModel savedUser = userRepository.save(user);

        logger.debug("Successfully created user with ID: {}", savedUser.getUserId());
        return savedUser;
    }

    // Get all users
    public List<UserModel> getAllUsers() {
        logger.debug("Retrieving all users from the database");
        return userRepository.findAll();
    }

    // Get user by ID
    public Optional<UserModel> getUserById(String uuid) {
        logger.debug("Attempting to retrieve user with ID: {}", uuid);

        Optional<UserModel> userOpt = userRepository.findById(uuid);
        if (userOpt.isPresent()) {
            logger.debug("Successfully retrieved user with ID: {}", uuid);
        } else {
            logger.warn("User with ID: {} not found", uuid);
        }
        return userOpt;
    }

    // Delete user by ID
    public boolean deleteUser(String uuid) {
        logger.debug("Attempting to delete user with ID: {}", uuid);
        if (userRepository.existsById(uuid)) {
            userRepository.deleteById(uuid);
            logger.debug("Successfully deleted user with ID: {}", uuid);
            return true;
        }

        logger.warn("User with ID: {} not found, cannot delete", uuid);
        return false;
    }

    // Update user
    public Optional<UserModel> updateUser(String uuid, UserModel updatedUser) {
        logger.debug("Attempting to update user with ID: {}", uuid);
        return userRepository.findById(uuid).map(user -> {
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            
            // Update other fields as necessary
            logger.debug("Successfully updated user with ID: {}", uuid);
            return userRepository.save(user);
        });
    }
}
