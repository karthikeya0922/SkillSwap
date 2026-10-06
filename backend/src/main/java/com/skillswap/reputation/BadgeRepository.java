package com.skillswap.reputation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

	Optional<Badge> findByCode(BadgeCode code);
}
