package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.ContactMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, Long> {
    List<ContactMessageEntity> findAllByOrderByCreatedAtDesc();
}
