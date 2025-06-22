package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    private String patronymic;
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
    @NotBlank(message = "Password is required")
    private String password;
}
