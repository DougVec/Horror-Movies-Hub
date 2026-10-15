package com.douglas.horrorhub.alerta;

import com.douglas.horrorhub.auth.UsuarioRepository;
import com.douglas.horrorhub.filme.FilmeController.FilmeResponse;
import com.douglas.horrorhub.filme.FilmeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/** Sem service: as regras são "não duplicar" e "não pedir estreia de filme já lançado". */
@RestController
@RequestMapping("/alertas")
public class AlertaController {

    public record AlertaResponse(Long id, FilmeResponse filme, Alerta.Tipo tipo, boolean ativo, LocalDateTime notificadoEm) {
        static AlertaResponse de(Alerta a) {
            return new AlertaResponse(a.getId(), FilmeResponse.de(a.getFilme()), a.getTipo(), a.isAtivo(), a.getNotificadoEm());
        }
    }

    private final AlertaRepository alertas;
    private final FilmeRepository filmes;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public AlertaController(AlertaRepository alertas, FilmeRepository filmes, UsuarioRepository usuarios, Clock clock) {
        this.alertas = alertas;
        this.filmes = filmes;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AlertaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return alertas.findByUsuarioIdOrderByIdDesc(usuarioId(jwt)).stream().map(AlertaResponse::de).toList();
    }

    @PostMapping("/{filmeId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AlertaResponse criar(@AuthenticationPrincipal Jwt jwt, @PathVariable long filmeId,
                                @RequestParam Alerta.Tipo tipo) {
        long usuarioId = usuarioId(jwt);
        var filme = filmes.findById(filmeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "filme não encontrado"));
        if (tipo == Alerta.Tipo.ESTREIA && filme.getDataLancamento() != null
                && !filme.getDataLancamento().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "este filme já estreou");
        }
        if (alertas.existsByUsuarioIdAndFilmeIdAndTipo(usuarioId, filmeId, tipo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "você já tem esse alerta");
        }
        return AlertaResponse.de(alertas.save(new Alerta(usuarios.getReferenceById(usuarioId), filme, tipo)));
    }

    /** Sem tipo, remove os dois. */
    @DeleteMapping("/{filmeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable long filmeId,
                        @RequestParam(required = false) Alerta.Tipo tipo) {
        var tipos = tipo == null ? EnumSet.allOf(Alerta.Tipo.class) : EnumSet.of(tipo);
        if (alertas.remover(usuarioId(jwt), filmeId, tipos) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "alerta não encontrado");
        }
    }

    private static long usuarioId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
