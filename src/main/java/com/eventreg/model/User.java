package com.eventreg.model;

import com.eventreg.model.enums.RBAC.Role;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Column(nullable = false, unique = true)
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  @Column(nullable = false, unique = true)
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  @Column(nullable = false)
  private String password;

  @NotNull(message = "Role is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private Role role = Role.PARTICIPANT;

  @JsonManagedReference
  @Builder.Default
  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Participant participant = null;
}
