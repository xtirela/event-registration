package com.eventreg.participantservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/** JPA entity representing an idempotency key record. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

  @Id private String key;

  private String requestMethod;
  private String requestPath;

  @Enumerated(EnumType.STRING)
  private HttpStatus httpStatus;

  @Column(columnDefinition = "TEXT")
  private String responseBody;

  private OffsetDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = OffsetDateTime.now();
  }
}
