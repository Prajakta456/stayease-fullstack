package com.stayease.entity;

import com.stayease.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    // Embedded in every JWT; incrementing it (on logout) invalidates all of the user's issued tokens
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer tokenVersion = 0;
}
