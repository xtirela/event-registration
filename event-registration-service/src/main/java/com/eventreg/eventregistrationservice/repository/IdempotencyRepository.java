package com.eventreg.eventregistrationservice.repository;

import com.eventreg.eventregistrationservice.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for idempotency records with a native claim operation. */
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {

  @Modifying
  @Query(
      value =
          "INSERT INTO idempotency_keys (key, request_method, request_path, http_status) "
              + "VALUES (:key, :method, :path, 'OK') ON CONFLICT (key) DO NOTHING",
      nativeQuery = true)
  int claim(@Param("key") String key, @Param("method") String method, @Param("path") String path);
}
