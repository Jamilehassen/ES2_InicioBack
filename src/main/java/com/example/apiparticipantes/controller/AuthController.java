package com.example.apiparticipantes.controller;

import com.example.apiparticipantes.dto.*;
import com.example.apiparticipantes.model.*;
import com.example.apiparticipantes.repository.*;
import com.example.apiparticipantes.security.JwtTokenProvider;

import com.example.apiparticipantes.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.apiparticipantes.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

import java.util.Random;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final ParticipanteRepository participanteRepository;
    private final EnderecoRepository enderecoRepository;
    private final BairroRepository bairroRepository;
    private final CidadeRepository cidadeRepository;
    private final UnidadeFederacaoRepository unidadeFederacaoRepository;
    private final LogradouroRepository logradouroRepository;
    private final TipoLogradouroRepository tipoLogradouroRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    private final TokenBlacklistService tokenBlacklistService;

    private static final List<String> ALLOWED_EMAIL_DOMAINS = Arrays.asList(
            "gmail.com",
            "hotmail.com",
            "outlook.com",
            "yahoo.com",
            "live.com",
            "icloud.com",
            "unioeste.br"
    );

    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthController(ParticipanteRepository participanteRepository,
                          EnderecoRepository enderecoRepository,
                          BairroRepository bairroRepository,
                          CidadeRepository cidadeRepository,
                          UnidadeFederacaoRepository unidadeFederacaoRepository,
                          LogradouroRepository logradouroRepository,
                          TipoLogradouroRepository tipoLogradouroRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          TokenBlacklistService tokenBlacklistService,
                          PasswordResetTokenRepository tokenRepository,
                          EmailService emailService) {
        this.participanteRepository = participanteRepository;
        this.enderecoRepository = enderecoRepository;
        this.bairroRepository = bairroRepository;
        this.cidadeRepository = cidadeRepository;
        this.unidadeFederacaoRepository = unidadeFederacaoRepository;
        this.logradouroRepository = logradouroRepository;
        this.tipoLogradouroRepository = tipoLogradouroRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (participanteRepository.existsByEmailParticipante(request.getEmailParticipante())) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        if (request.getCargo() == Cargo.ADMIN) {
            return ResponseEntity.badRequest().body("Não é possível se registrar como ADMIN.");
        }

        String email = request.getEmailParticipante();
        if (email == null || !email.contains("@")) {
            return ResponseEntity.badRequest().body("Formato de e-mail inválido.");
        }
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        if (!ALLOWED_EMAIL_DOMAINS.contains(domain)) {
            return ResponseEntity.badRequest().body("Domínio de e-mail não permitido. Use um e-mail de um fornecedor conhecido (Gmail, Hotmail, etc.).");
        }

        if (participanteRepository.existsByEmailParticipante(request.getEmailParticipante())) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        // ====== UF ======
        UnidadeFederacao uf = unidadeFederacaoRepository.findById(request.getSiglaUf())
                .orElseGet(() -> {
                    UnidadeFederacao nova = new UnidadeFederacao();
                    nova.setSiglaUf(request.getSiglaUf());
                    nova.setNomeUf(request.getSiglaUf()); // ou use request.getNomeUf() se tiver
                    return unidadeFederacaoRepository.save(nova);
                });

        // ====== Cidade ======
        Cidade cidade = cidadeRepository.findByNomeCidade(request.getNomeCidade())
                .orElseGet(() -> {
                    Cidade nova = new Cidade();
                    nova.setIdCidade(UUID.randomUUID().toString());
                    nova.setNomeCidade(request.getNomeCidade());
                    nova.setUnidadeFederacao(uf);
                    return cidadeRepository.save(nova);
                });

        // ====== Bairro ======
        Bairro bairro = bairroRepository.findByNomeBairro(request.getNomeBairro())
                .orElseGet(() -> {
                    Bairro novo = new Bairro();
                    novo.setIdBairro(UUID.randomUUID().toString());
                    novo.setNomeBairro(request.getNomeBairro());
                    return bairroRepository.save(novo);
                });

        // ====== Tipo Logradouro ======
        TipoLogradouro tipo = tipoLogradouroRepository.findByNomeTipoLogradouro(request.getNomeTipoLogradouro())
                .orElseGet(() -> {
                    TipoLogradouro novo = new TipoLogradouro();
                    novo.setIdTipoLogradouro(UUID.randomUUID().toString());
                    novo.setNomeTipoLogradouro(request.getNomeTipoLogradouro());
                    return tipoLogradouroRepository.save(novo);
                });

        // ====== Logradouro ======
        Logradouro logradouro = logradouroRepository.findByNomeLogradouro(request.getNomeLogradouro())
                .orElseGet(() -> {
                    Logradouro novo = new Logradouro();
                    novo.setIdLogradouro(UUID.randomUUID().toString());
                    novo.setNomeLogradouro(request.getNomeLogradouro());
                    novo.setTipoLogradouro(tipo);
                    return logradouroRepository.save(novo);
                });

        // ====== Endereço ======
        Endereco endereco = new Endereco();
        endereco.setCep(request.getCep());
        endereco.setComplemento(request.getComplemento());
        endereco.setNumero(request.getNumero());
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setLogradouro(logradouro);
        enderecoRepository.save(endereco);

        // ====== Participante ======
        Participante p = new Participante();
        p.setIdParticipante(UUID.randomUUID().toString());
        p.setNomeParticipante(request.getNomeParticipante());
        p.setEmailParticipante(request.getEmailParticipante());
        p.setTelefoneParticipante(request.getTelefoneParticipante());
        p.setSenhaParticipante(passwordEncoder.encode(request.getSenhaParticipante()));
        p.setCargo(request.getCargo());
        p.setEndereco(endereco);

        participanteRepository.save(p);

        return ResponseEntity.ok("Registrado com sucesso");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha());
        var auth = authenticationManager.authenticate(authToken);
        String token = tokenProvider.generateToken(request.getEmail());
        return ResponseEntity.ok(new JwtResponse(token, request.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }

        if (token != null) {
            tokenBlacklistService.adicionarTokenNaBlacklist(token);
            // Poderíamos extrair a data de expiração do token e guardá-lo na blacklist
            // apenas até essa data para otimizar o espaço.
        }

        return ResponseEntity.ok("Logout realizado com sucesso.");
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Participante participante = participanteRepository.findByEmailParticipante(request.getEmail())
                .orElse(null);

        boolean emailSent = false;

        if (participante != null && participante.isAtivo()) {
            tokenRepository.deleteByParticipanteIdParticipante(participante.getIdParticipante());

            Random rnd = new Random();
            int number = rnd.nextInt(900000) + 100000;
            String codeValue = String.valueOf(number);

            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15); // Código válido por 15 minutos

            PasswordResetToken resetCode = new PasswordResetToken(codeValue, participante, expiryDate);
            tokenRepository.save(resetCode);

            // Tenta enviar e-mail com o CÓDIGO
            emailSent = emailService.sendPasswordResetEmail(participante.getEmailParticipante(), codeValue);

            // Removida a parte do SMS
            if (!emailSent) {
                // logger.error("Falha ao enviar e-mail de redefinição para {}", participante.getEmailParticipante());
            }
        }

        return ResponseEntity.ok("Se o e-mail estiver registado e ativo, receberá instruções para redefinir a senha via E-mail."); // Mensagem ajustada
    }

    /**
     * Rota para redefinir a senha usando o token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetCode = tokenRepository.findByCode(request.getCode()) // Deve ser findByCode
                .orElse(null);

        if (resetCode == null || resetCode.isExpired()) {
            return ResponseEntity.badRequest().body("Código inválido ou expirado.");
        }

        // Verifica se a nova senha é válida (adicione mais validações se necessário)
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest().body("A nova senha deve ter pelo menos 6 caracteres.");
        }

        Participante participante = resetCode.getParticipante();
        // Codifica a nova senha antes de salvar
        participante.setSenhaParticipante(passwordEncoder.encode(request.getNewPassword()));
        participanteRepository.save(participante);

        // Remove o token após o uso
        tokenRepository.delete(resetCode);

        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }
}
