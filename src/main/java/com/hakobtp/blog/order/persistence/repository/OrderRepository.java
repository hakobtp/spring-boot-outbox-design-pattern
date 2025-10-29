package com.hakobtp.blog.order.persistence.repository;

import com.hakobtp.blog.order.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}
