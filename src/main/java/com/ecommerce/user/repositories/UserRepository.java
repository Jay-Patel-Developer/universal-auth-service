package com.ecommerce.user.repositories;

import com.ecommerce.user.models.User;
import com.ecommerce.user.models.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    Optional<User> findByEmailAndStatus(String email, UserStatus status);
    List<User> findByStatus(UserStatus status);
    // GDPR-related queries
    @Query("SELECT u FROM User u WHERE u.scheduledDeletionAt IS NOT NULL AND u.scheduledDeletionAt <= :cutoffDate")
    List<User> findUsersScheduledForDeletion(@Param("cutoffDate") LocalDateTime cutoffDate);
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt <= :cutoffDate")
    List<User> findDeletedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    @Query("SELECT u FROM User u WHERE u.deletionRequestedAt IS NOT NULL AND u.deletionRequestedAt <= :cutoffDate AND u.scheduledDeletionAt IS NULL")
    List<User> findUsersWithExpiredDeletionRequests(@Param("cutoffDate") LocalDateTime cutoffDate);
    @Query("SELECT u FROM User u WHERE u.mfaEnabled = true")
    List<User> findUsersWithMfaEnabled();
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersRegisteredSince(@Param("startDate") LocalDateTime startDate);
    @Query("SELECT u FROM User u WHERE u.lastLogin IS NULL OR u.lastLogin <= :cutoffDate")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
}
