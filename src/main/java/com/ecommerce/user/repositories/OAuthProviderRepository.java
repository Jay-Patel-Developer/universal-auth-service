package com.ecommerce.user.repositories;

import com.ecommerce.user.models.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for OAuth provider entities.
 * Provides method to find provider by name and ID.
 */
public interface OAuthProviderRepository extends JpaRepository<OAuthProvider, Long> {
    /**
     * Find OAuth provider by provider name and provider ID.
     * @param providerName Name of the OAuth provider (e.g., Google, Facebook)
     * @param providerId Provider-specific user ID
     * @return OAuthProvider entity if found, otherwise null
     */
    OAuthProvider findByProviderNameAndProviderId(String providerName, String providerId);
}
