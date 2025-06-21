package com.example.bankcards.dto;

import lombok.Data;

@Data
public class UserRegisterRequest {
    private String firstName;
    private String lastName;
    private String patronymic;
    private String email;
    private String mobileNumber;
    private String password;
}
