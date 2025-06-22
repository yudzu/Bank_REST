package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String mobileNumber;
    private Set<String> roles;
}
