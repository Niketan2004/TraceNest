package com.tracenest.backend.Subscription.repository;

import com.tracenest.backend.Subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for authenticated user {@link Subscription} entities.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * Finds all alert subscriptions for a given user.
     *
     * @param userId The account user ID.
     * @return List of matching subscriptions.
     */
    List<Subscription> findByUserId(String userId);

    /**
     * Finds all alert subscriptions for a user ordered by creation timestamp descending.
     *
     * @param userId The account user ID.
     * @return List of matching subscriptions sorted newest first.
     */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(String userId);
}
