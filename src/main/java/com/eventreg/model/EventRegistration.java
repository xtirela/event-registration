package com.eventreg.model;

import com.eventreg.model.enums.EventRegistrationStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.*;

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

  @JsonBackReference
  @NotNull(message = "Event is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @NotNull(message = "Participant is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "participant_id", nullable = false)
  private Participant participant;

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
