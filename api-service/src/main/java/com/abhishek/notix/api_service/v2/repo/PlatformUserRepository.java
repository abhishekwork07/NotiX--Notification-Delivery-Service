package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, UUID> {

    Optional<PlatformUser> findByExternalUserId(String externalUserId);

    Optional<PlatformUser> findByEmail(String email);

    Optional<PlatformUser> findByEmailIgnoreCase(String email);

    Optional<PlatformUser> findByUsernameIgnoreCase(String username);
}
