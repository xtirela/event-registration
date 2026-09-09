package com.eventreg.participantservice.model;

import com.eventreg.participantservice.model.enums.ParticipantGender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing a participant profile. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participants")
public class Participant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Keycloak ID is required")
  @Column(name = "keycloak_id", nullable = false, unique = true)
  private String keycloakId;

  @NotBlank(message = "First name is required")
  @Column(nullable = false)
  private String firstName;

  @Size(max = 100, message = "Last name too long")
  private String lastName;

  @Min(value = 1, message = "Age must be greater than zero")
  @Max(value = 150, message = "Age too high")
  private Integer age;

  @NotNull(message = "Gender is required")
  @Enumerated(EnumType.STRING)
  private ParticipantGender participantGender;
}
