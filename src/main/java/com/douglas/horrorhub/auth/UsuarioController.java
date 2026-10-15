package com.douglas.horrorhub.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/usuarios/me")
public class UsuarioController {

    /** chat_id do Telegram é numérico (negativo para grupos). */
    public record TelegramRequest(@NotBlank @Pattern(regexp = "-?\\d{1,20}") String chatId) {}

    private final UsuarioRepository usuarios;

    public UsuarioController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @PutMapping("/telegram")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void telegram(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TelegramRequest req) {
        usuarios.findById(Long.parseLong(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado"))
                .setChatIdTelegram(req.chatId());
    }
}
