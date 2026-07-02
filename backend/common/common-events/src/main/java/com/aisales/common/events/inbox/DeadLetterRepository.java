package com.aisales.common.events.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetterMessage, UUID> {
}
