package com.example.apiparticipantes.controller;

import com.example.apiparticipantes.dto.*;
import com.example.apiparticipantes.model.Participante;
import com.example.apiparticipantes.repository.ParticipanteRepository;
import com.example.apiparticipantes.security.JwtTokenProvider;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ParticipanteRepository participanteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(ParticipanteRepository participanteRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider) {
        this.participanteRepository = participanteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (participanteRepository.existsByEmailParticipante(request.getEmailParticipante())) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        Participante p = new Participante();
        p.setIdParticipante(UUID.randomUUID().toString());
        p.setNomeParticipante(request.getNomeParticipante());
        p.setEmailParticipante(request.getEmailParticipante());
        p.setTelefoneParticipante(request.getTelefoneParticipante());
        p.setSenhaParticipante(passwordEncoder.encode(request.getSenhaParticipante()));
        p.setCargo(request.getCargo());
        // cargo/endereco podem ser setados opcionalmente
        participanteRepository.save(p);

        return ResponseEntity.ok("Registrado com sucesso");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha());
        var auth = authenticationManager.authenticate(authToken); // lança exception se falhar
        String token = tokenProvider.generateToken(request.getEmail());
        return ResponseEntity.ok(new JwtResponse(token, request.getEmail()));
    }
}
