package com.douglas.horrorhub.auth;

import com.douglas.horrorhub.shared.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final SecurityConfig config = new SecurityConfig("segredo-de-teste-com-mais-de-32-bytes!!");
    private final AuthService service = new AuthService(repo, encoder, config.jwtEncoder(), Duration.ofHours(2));

    private Usuario salvo(String senha) {
        var u = new Usuario("Ana", "ana@x.com", encoder.encode(senha));
        ReflectionTestUtils.setField(u, "id", 7L);
        return u;
    }

    @Test
    void registroGuardaHashENormalizaEmail() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var u = service.registrar(" Ana ", " Ana@X.com ", "senha-forte");
        assertEquals("ana@x.com", u.getEmail());
        assertEquals("Ana", u.getNome());
        assertNotEquals("senha-forte", u.getSenha());
        assertTrue(encoder.matches("senha-forte", u.getSenha()));
    }

    @Test
    void emailDuplicadoDa409() {
        when(repo.existsByEmail("ana@x.com")).thenReturn(true);
        var e = assertThrows(ResponseStatusException.class, () -> service.registrar("Ana", "ana@x.com", "senha-forte"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void loginValidoGeraTokenComIdEPapel() {
        when(repo.findByEmail("ana@x.com")).thenReturn(Optional.of(salvo("senha-forte")));
        var token = service.login("ANA@x.com", "senha-forte");
        var jwt = config.jwtDecoder().decode(token);
        assertEquals("7", jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("scope"));
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
    }

    @Test
    void senhaErradaEEmailInexistenteDaoMesmo401() {
        when(repo.findByEmail("ana@x.com")).thenReturn(Optional.of(salvo("senha-forte")));
        var senhaErrada = assertThrows(ResponseStatusException.class, () -> service.login("ana@x.com", "outra"));
        var semConta = assertThrows(ResponseStatusException.class, () -> service.login("ninguem@x.com", "outra"));
        assertEquals(HttpStatus.UNAUTHORIZED, senhaErrada.getStatusCode());
        assertEquals(senhaErrada.getReason(), semConta.getReason());
    }

    @Test
    void tokenDeOutroSegredoERejeitado() {
        when(repo.findByEmail("ana@x.com")).thenReturn(Optional.of(salvo("senha-forte")));
        var token = service.login("ana@x.com", "senha-forte");
        var outro = new SecurityConfig("um-segredo-completamente-diferente-123456").jwtDecoder();
        assertThrows(Exception.class, () -> outro.decode(token));
    }
}
