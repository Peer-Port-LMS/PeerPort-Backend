package peerport.backend.database;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.UserModel;

@Repository
public interface UsersRepository extends JpaRepository<UserModel, String> {
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByProviderAndProviderId(String provider, String providerId);
}