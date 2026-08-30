package com.eventreg.model;

import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
@Entity
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Event name is required")
  @Column(nullable = false)
  private String eventName;

  @Size(max = 1000, message = "Description too long")
  private String eventDescription;

  @NotNull(message = "Event date is required")
  private OffsetDateTime eventDate;

  @NotNull(message = "event duration is required")
  private Duration eventDuration;

  @NotBlank(message = "Location is required")
  private String location;

  @Min(value = 0, message = "Age cannot be negative")
  private int ageRequired;

  @NotNull(message = "Gender requirement is required")
  @Enumerated(EnumType.STRING)
  private EventGenderRequirement eventGenderRequirement;

  @Min(value = 0, message = "Current participant amount cannot be negative")
  @Builder.Default
  private int currentParticipantAmount = 0;

  @Min(value = 0, message = "Current participant amount in queue cannot be negative")
  @Builder.Default
  private int currentWaitingQueueParticipantAmount = 0;

  @Min(value = 1, message = "Max participants must be at least 1")
  private int maxParticipantAmount;

  @NotNull(message = "event status is required")
  @Builder.Default
  @Enumerated(EnumType.STRING)
  private EventStatus eventStatus = EventStatus.PLANNED;

  @NotNull(message = "event reservation status is required")
  @Builder.Default
  @Enumerated(EnumType.STRING)
  private EventReservationStatus eventReservationStatus = EventReservationStatus.RESERVATIONS_OPEN;

  private boolean confirmationRequired;
  private boolean waitlistWhenAllReserved;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  @JsonManagedReference
  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  List<EventRegistration> eventRegistrations = new ArrayList<>();

  @NotNull(message = "event must have an organizer")
  @ManyToOne
  @JoinColumn(name = "organizer_id", nullable = false)
  User organizer;

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
// TODO: Добавить логику взаимодействия с временем, отдельная логика времени куда вводятся данные и
// проверяются какие события происходят и какие будут происходить
// TODO: окно регистрации на событие
// TODO: автоматизировання логики EventReservationStatus связанная с текущим временем, то же самое
// с EventStatus
// TODO: EventService и polling его на обновление статуса События
