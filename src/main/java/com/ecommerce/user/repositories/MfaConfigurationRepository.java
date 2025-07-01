package com.ecommerce.user.repositories;

import com.ecommerce.user.models.MfaConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for MFA configuration entities.
 * Provides methods to find and check MFA configuration by user.
 */
@Repository
public interface MfaConfigurationRepository extends JpaRepository<MfaConfiguration, Long> {
    /**
     * Find MFA configuration by user ID.
     * @param userId User ID
     * @return Optional containing MFA configuration if found
     */
    Optional<MfaConfiguration> findByUserId(Long userId);

    /**
     * Check if an enabled MFA configuration exists for a user.
     * @param userId User ID
     * @param enabled Whether MFA is enabled
     * @return true if exists, false otherwise
     */
    boolean existsByUserIdAndEnabled(Long userId, boolean enabled);
}
