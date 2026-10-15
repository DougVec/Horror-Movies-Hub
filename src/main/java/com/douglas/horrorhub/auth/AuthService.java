package com.douglas.horrorhub.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwt;
    private final Duration expiracao;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtEncoder jwt,
                       @Value("${jwt.expiracao}") Duration expiracao) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.expiracao = expiracao;
    }

    @Transactional
    public Usuario registrar(String nome, String email, String senha) {
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "e-mail já cadastrado");
        }
        return usuarios.save(new Usuario(nome.trim(), emailNormalizado, encoder.encode(senha)));
    }

    /** Mesma resposta para e-mail inexistente e senha errada, para não revelar quem tem conta. */
    public String login(String email, String senha) {
        var usuario = usuarios.findByEmail(email.trim().toLowerCase())
                .filter(u -> encoder.matches(senha, u.getSenha()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "credenciais inválidas"));
        return gerarToken(usuario);
    }

    private String gerarToken(Usuario u) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("horrorhub")
                .subject(u.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao))
                .claim("scope", u.getPapel().name())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwt.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
