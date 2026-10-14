package com.codingshuttle.projects.airBnbApp.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class HotelContactInfo {
    @Size(max = 255)
    private String address;

    @Size(max = 30)
    private String phoneNumber;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String location;
}
