package com.hakobtp.blog.outbox.persistence.repository;

import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEventEntity, Long> {

    Page<OutboxEventEntity> findAllByEventType(OutboxEventType eventType, Pageable pageable);
}
