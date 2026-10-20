package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    @Modifying
    @Query("delete from AuthToken t where t.user.id = :userId and t.purpose = :purpose and t.consumedAt is null")
    void deleteActive(@Param("userId") Long userId, @Param("purpose") AuthToken.Purpose purpose);

    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :cutoff or t.consumedAt < :cutoff")
    void deleteOld(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("update AuthToken t set t.consumedAt = :now where t.tokenHash = :hash and t.purpose = :purpose and t.consumedAt is null and t.expiresAt > :now")
    int consume(@Param("hash") String hash, @Param("purpose") AuthToken.Purpose purpose, @Param("now") Instant now);

    Optional<AuthToken> findByTokenHash(String hash);
}
