package com.eventreg.eventregistrationservice.model;

import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing a single event registration. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_registrations")
@Entity
public class EventRegistration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "Registration status is required")
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventRegistrationStatus eventRegistrationStatus = EventRegistrationStatus.PENDING;

  @Size(max = 500, message = "Description too long")
  @Builder.Default
  private String description = "none";

  @NotNull(message = "Event id is required")
  private Long eventId;

  @NotNull(message = "Participant id is required")
  private Long participantId;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  @PrePersist
  void onSave() {
    createdAt = OffsetDateTime.now();
    updatedAt = OffsetDateTime.now();
  }

  @PostUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
