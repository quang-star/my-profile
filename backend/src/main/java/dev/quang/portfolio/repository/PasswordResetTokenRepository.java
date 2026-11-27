package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.PasswordResetTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
    List<PasswordResetTokenEntity> findByUserIdAndUsedAtIsNull(String userId);
}
