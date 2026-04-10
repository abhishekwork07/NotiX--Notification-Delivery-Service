package com.abhishek.notix.email_sender_service.repo;

import com.abhishek.notix.email_sender_service.model.ProviderAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderAccountRepository extends JpaRepository<ProviderAccount, UUID> {

    Optional<ProviderAccount> findByIdAndTenantId(UUID id, UUID tenantId);
}
