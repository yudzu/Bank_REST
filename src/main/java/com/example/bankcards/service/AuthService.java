package com.example.bankcards.service;

import com.example.bankcards.dto.UserLoginRequest;
import com.example.bankcards.exception.UserLoginException;
import com.example.bankcards.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public String login(UserLoginRequest request) throws UserLoginException {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return jwtProvider.generateToken(userDetails);
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            throw new UserLoginException(e.getMessage());
        } catch (Exception e) {
            throw new UserLoginException("Authentication error");
        }
    }
}
