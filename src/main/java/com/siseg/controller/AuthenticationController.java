package com.siseg.controller;

import com.siseg.dto.authentication.AuthenticationDTO;
import com.siseg.service.AuthenticationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestBody AuthenticationDTO request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword());
        return authenticationService.authenticate(authentication);
    }
}


