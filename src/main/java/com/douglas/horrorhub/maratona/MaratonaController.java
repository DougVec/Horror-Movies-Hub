package com.douglas.horrorhub.maratona;

import com.douglas.horrorhub.filme.FilmeController.FilmeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/maratonas")
public class MaratonaController {

    /** tempo máximo de um dia: mantém a tabela da programação dinâmica pequena. */
    public record GerarRequest(@NotNull @Min(1) @Max(1440) Integer tempoMin,
                               @Min(1) @Max(5) Integer nivelMedoMin,
                               Boolean apenasQueroVer) {}

    public record MaratonaResponse(Long id, int tempoDisponivelMin, int nivelMedoMin, int duracaoTotal,
                                   int tempoSobrando, LocalDateTime criadaEm, List<FilmeResponse> filmes) {
        static MaratonaResponse de(Maratona m) {
            return new MaratonaResponse(m.getId(), m.getTempoDisponivelMin(), m.getNivelMedoMin(), m.getDuracaoTotal(),
                    m.getTempoDisponivelMin() - m.getDuracaoTotal(), m.getCriadaEm(),
                    m.getFilmes().stream().map(FilmeResponse::de).toList());
        }
    }

    private final MaratonaService service;

    public MaratonaController(MaratonaService service) {
        this.service = service;
    }

    @PostMapping("/gerar")
    @ResponseStatus(HttpStatus.CREATED)
    public MaratonaResponse gerar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GerarRequest req) {
        return MaratonaResponse.de(service.gerar(Long.parseLong(jwt.getSubject()), req.tempoMin(),
                req.nivelMedoMin() == null ? 1 : req.nivelMedoMin(),
                req.apenasQueroVer() == null || req.apenasQueroVer()));
    }

    @GetMapping
    public List<MaratonaResponse> historico(@AuthenticationPrincipal Jwt jwt) {
        return service.historico(Long.parseLong(jwt.getSubject())).stream().map(MaratonaResponse::de).toList();
    }
}
