package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Run every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpired() {
        Instant now = Instant.now();
        long deleted = refreshTokenRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            System.out.println("[RefreshTokenCleanup] Deleted expired tokens: " + deleted);
        }
    }
}
