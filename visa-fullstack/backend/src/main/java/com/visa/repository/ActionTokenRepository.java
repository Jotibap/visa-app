package com.visa.repository;

import com.visa.entity.ActionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ActionTokenRepository extends JpaRepository<ActionToken, Long> {
    Optional<ActionToken> findByTokenAndPurposeAndConsumedFalse(String token, String purpose);

    void deleteByUserIdAndPurpose(Long userId, String purpose);
}
