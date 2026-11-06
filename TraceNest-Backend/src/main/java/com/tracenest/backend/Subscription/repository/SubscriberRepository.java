package com.tracenest.backend.Subscription.repository;

import com.tracenest.backend.Subscription.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for public {@link Subscriber} entity operations.
 */
@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, String> {

    /**
     * Looks up an existing subscriber matching either email or mobile number.
     *
     * @param email Email address to search.
     * @param mobileNumber Mobile number to search.
     * @return {@link Optional} containing matching subscriber if found.
     */
    Optional<Subscriber> findByEmailOrMobileNumber(String email, String mobileNumber);

    /**
     * Finds subscriber by exact email address.
     *
     * @param email Target email.
     * @return {@link Optional} of Subscriber.
     */
    Optional<Subscriber> findByEmail(String email);

    /**
     * Finds subscriber by exact mobile number.
     *
     * @param mobileNumber Target mobile number.
     * @return {@link Optional} of Subscriber.
     */
    Optional<Subscriber> findByMobileNumber(String mobileNumber);
}
