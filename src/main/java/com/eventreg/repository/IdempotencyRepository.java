package com.eventreg.repository;

import com.eventreg.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {}
