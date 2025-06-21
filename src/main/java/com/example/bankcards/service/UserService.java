package com.example.bankcards.service;

import com.example.bankcards.dto.UserRegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserRegisterException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(UserRegisterRequest request) throws UserRegisterException {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserRegisterException("User with this email already exists!");
        }

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .patronymic(request.getPatronymic())
                .hashPassword(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(getDefaultUserRole()))
                .build();

        userRepository.save(user);
    }

    private Role getDefaultUserRole() {
        return roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new UserRegisterException("Default role USER not found"));
    }
}
