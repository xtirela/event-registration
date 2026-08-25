package com.eventreg.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.eventreg.model.enums.EventRegRequestStatus;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="event_registrations")
@Entity
public class EventRegistration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private EventRegRequestStatus eventRegRequestStatus;

  @Builder.Default private String description = "none";

  @ManyToOne
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne
  @JoinColumn(name = "participant_id")
  private Participant participant;

  private OffsetDateTime createdAt;
}
