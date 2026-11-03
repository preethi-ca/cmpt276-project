package com.cmpt276.project.owner.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SalonJpaRepository extends JpaRepository<SalonEntity, String> {

    Optional<SalonEntity> findByOwnerId(String ownerId);

    Optional<SalonEntity> findBySlug(String slug);
}