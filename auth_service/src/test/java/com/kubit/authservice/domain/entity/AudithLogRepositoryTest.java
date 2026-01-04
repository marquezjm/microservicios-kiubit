package com.kubit.authservice.domain.entity;

import com.kubit.authservice.domain.repository.AudithLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class AudithLogRepositoryTest {
    @Autowired
    private AudithLogRepository logRepo;

    @Test
    void canPersistAuditLog() {
        AudithLog log = AudithLog.builder()
                .authUserId(1L)
                .eventType("LOGIN")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();
        AudithLog saved = logRepo.save(log);
        assertNotNull(saved.getId());
        assertEquals("LOGIN", saved.getEventType());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }
}
