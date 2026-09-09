package com.eventreg.eventservice.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.springframework.http.HttpStatus;

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
