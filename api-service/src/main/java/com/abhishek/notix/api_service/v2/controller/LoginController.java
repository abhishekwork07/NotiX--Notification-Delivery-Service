package com.abhishek.notix.api_service.v2.controller;

import com.abhishek.notix.api_service.v2.dto.JwtAuthResponse;
import com.abhishek.notix.api_service.v2.dto.LoginRequest;
import com.abhishek.notix.api_service.v2.service.LocalJwtAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    private final LocalJwtAuthService localJwtAuthService;

    public LoginController(LocalJwtAuthService localJwtAuthService) {
        this.localJwtAuthService = localJwtAuthService;
    }

    @PostMapping({"/auth/login", "/v2/auth/login"})
    public JwtAuthResponse login(@Valid @RequestBody LoginRequest request) {
        return localJwtAuthService.login(request);
    }
}
