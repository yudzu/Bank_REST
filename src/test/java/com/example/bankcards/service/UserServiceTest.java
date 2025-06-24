package com.example.bankcards.service;

import com.example.bankcards.dto.CreateUserRequest;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.RoleNotFoundException;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .patronymic(null)
                .mobileNumber("1234567890")
                .roles(Set.of(new Role(1L, RoleName.USER)))
                .build();
    }

    @Test
    void getAllUsers_ReturnsUserDtoPage() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        Page<UserDto> result = userService.getAllUsers(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("user@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getUserById_WhenExists_ReturnsUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById(1L);

        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    void getUserById_WhenNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void createUser_WhenUserExists_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(request));
    }

    @Test
    void createUser_Success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(new Role(1L, RoleName.USER)));
        when(passwordEncoder.encode("password")).thenReturn("hashed-password");

        userService.createUser(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WhenDefaultRoleMissing_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> userService.createUser(request));
    }

    @Test
    void deleteUser_InvokesRepository() {
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }
}
