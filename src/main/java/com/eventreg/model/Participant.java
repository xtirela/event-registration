package com.eventreg.model;

import com.eventreg.model.enums.ParticipantGender;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

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

  @NotBlank(message = "First name is required")
  @Column(nullable = false)
  private String firstName;

  @Size(max = 100, message = "Last name too long")
  private String lastName;

  @Min(value = 1, message = "Age must be greater than zero")
  @Max(value = 150, message = "Age too high")
  private int age;

  @JsonBackReference
  @NotNull(message = "User is required")
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @NotNull(message = "Gender is required")
  @Enumerated(EnumType.STRING)
  private ParticipantGender participantGender;
}
