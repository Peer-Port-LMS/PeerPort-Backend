package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.UsersRepository;
import peerport.backend.model.UserModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class UserService {
    protected static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UsersRepository userRepository;

    /**
     * Create a new user in the database.
     * @param user - The user model containing the details of the user to be created.
     * @return The saved user model with the generated ID.
     */
    public UserModel createUser(UserModel user) {
        logger.debug("Attempting to create user with email: {}", user.getEmail());
        UserModel savedUser = userRepository.save(user);

        logger.debug("Successfully created user with ID: {}", savedUser.getUserId());
        return savedUser;
    }

    /**
     * Retrieve all users from the database.
     * @return A list of all user models in the database.
     */
    public List<UserModel> getAllUsers() {
        logger.debug("Retrieving all users from the database");
        return userRepository.findAll();
    }

    /**
     * Retrieve a user by their unique ID.
     * @param uuid - The unique ID of the user to retrieve.
     * @return An Optional containing the user model if found, or empty if not found.
     */
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

    /**
     * Delete a user from the database by their unique ID.
     * @param uuid - The unique ID of the user to delete.
     * @return true if the user was deleted, false otherwise.
     */
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

    /**
     * Update an existing user's details in the database.
     * @param uuid - The unique ID of the user to update.
     * @param updatedUser - The user model containing the updated details of the user.
     * @return An Optional containing the updated user model if the update was successful, or empty if the user was not found.
     */
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
