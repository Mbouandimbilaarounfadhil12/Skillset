package com.skillset.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private String profilePictureUrl;

    // Champs de profil candidat (issus de l'onboarding, éditables depuis "Mon profil")
    private String jobDomain;
    private String desiredRole;
    private String experienceLevel;
    private String contractType;
    private String location;
    private String skills;
    private String bio;

    public UserDTO(String id, String email, String firstName, String lastName,
                   String phoneNumber, String role, String profilePictureUrl) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.profilePictureUrl = profilePictureUrl;
    }
}
