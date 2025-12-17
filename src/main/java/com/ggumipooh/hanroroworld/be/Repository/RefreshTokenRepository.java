package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
	long deleteByUserIdOrExpiresAtBefore(Long userId, Instant before);
	long deleteByExpiresAtBefore(Instant before);
}


