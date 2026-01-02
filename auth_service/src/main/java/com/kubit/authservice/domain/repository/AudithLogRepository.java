package com.kubit.authservice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kubit.authservice.domain.entity.AudithLog;

public interface AudithLogRepository extends JpaRepository<AudithLog, Long> {
  
}
