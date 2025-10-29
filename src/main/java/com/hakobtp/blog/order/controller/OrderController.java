package com.hakobtp.blog.order.controller;

import com.hakobtp.blog.order.controller.request.CreateOrUpdateOrderRequest;
import com.hakobtp.blog.order.controller.response.OrderResponse;
import com.hakobtp.blog.order.facade.OrderFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long orderId) {
        OrderResponse response = orderFacade.getById(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> search(Pageable pageable) {
        Page<OrderResponse> page = orderFacade.search(pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrUpdateOrderRequest request) {
        OrderResponse created = orderFacade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> update(
            @PathVariable Long orderId,
            @RequestBody CreateOrUpdateOrderRequest request) {
        OrderResponse updated = orderFacade.update(orderId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId) {
        orderFacade.delete(orderId);
        return ResponseEntity.noContent().build();
    }
}
