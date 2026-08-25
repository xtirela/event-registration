package com.eventreg.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventStatus;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
@Entity
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String eventName;

  private OffsetDateTime eventDate;

  private String location;

  private Duration eventDuration;

  private int ageRequired;

  private EventGenderRequirement eventGenderRequirement;

  private int currentParticipantAmount;
  private int maxParticipantAmount;

  private EventStatus eventStatus;
  private EventRegistrationStatus eventRegistrationStatus;

  private OffsetDateTime createdAt;

  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  List<EventRegistration> eventRegistration;

  @ManyToOne
  @JoinColumn(name = "organiser_id")
  User user;

}
// TODO: Добавить логику взаимодействия с временем, отдельная логика времени куда вводятся данные и
// проверяются какие события происходят и какие будут происходить
// TODO: окно регистрации на событие
// TODO: автоматизировання логики EventRegistrationStatus связанная с текущим временем, то же самое
// с EventStatus
// TODO: EventService и polling его на обновление статуса События
