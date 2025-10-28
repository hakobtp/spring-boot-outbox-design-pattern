package com.hakobtp.blog.outbox.persistence.repository;

import com.hakobtp.blog.outbox.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEventEntity, Long> {
}
