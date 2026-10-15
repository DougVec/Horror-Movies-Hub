package com.douglas.horrorhub.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record RegistroRequest(@NotBlank @Size(max = 100) String nome,
                                  @NotBlank @Email @Size(max = 255) String email,
                                  @NotBlank @Size(min = 8, max = 72) String senha) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {}

    public record UsuarioResponse(Long id, String nome, String email) {}

    public record TokenResponse(String token) {}

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest req) {
        var u = auth.registrar(req.nome(), req.email(), req.senha());
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return new TokenResponse(auth.login(req.email(), req.senha()));
    }
}
