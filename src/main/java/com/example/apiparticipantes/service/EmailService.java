package com.example.apiparticipantes.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Redefinição de Senha - Evento App");
        // Crie uma URL no seu frontend que receba o token
        // Ex: http://seu-frontend.com/reset-password?token=TOKEN_AQUI
        message.setText("Para redefinir a sua senha, use o seguinte token: " + token +
                "\nOu clique no link: [Link para o frontend com o token]"); // Adapte a mensagem e o link
        mailSender.send(message);
    }
}