package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.ProviderAccount;
import com.abhishek.notix.common.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderAccountRepository extends JpaRepository<ProviderAccount, UUID> {

    Optional<ProviderAccount> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ProviderAccount> findFirstByTenantIdAndChannelAndActiveTrueOrderByCreatedAtAsc(UUID tenantId, Channel channel);
}
