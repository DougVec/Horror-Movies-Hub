package com.douglas.horrorhub.review;

import com.douglas.horrorhub.auth.UsuarioRepository;
import com.douglas.horrorhub.filme.FilmeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Sem service: a única regra (uma review por usuário e filme) é um exists + a constraint única do banco. */
@RestController
@RequestMapping("/filmes/{filmeId}/reviews")
public class ReviewController {

    public record ReviewRequest(@Size(max = 5000) String texto,
                                @NotNull @Min(0) @Max(10) Integer nota,
                                @NotNull @Min(1) @Max(5) Integer medometro) {}

    public record ReviewResponse(Long id, String autor, String texto, int nota, int medometro, LocalDateTime data) {
        static ReviewResponse de(Review r) {
            return new ReviewResponse(r.getId(), r.getUsuario().getNome(), r.getTexto(), r.getNota(), r.getMedometro(), r.getData());
        }
    }

    public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {}

    private final ReviewRepository reviews;
    private final FilmeRepository filmes;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ReviewController(ReviewRepository reviews, FilmeRepository filmes, UsuarioRepository usuarios, Clock clock) {
        this.reviews = reviews;
        this.filmes = filmes;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReviewResponse criar(@AuthenticationPrincipal Jwt jwt, @PathVariable long filmeId,
                                @Valid @RequestBody ReviewRequest req) {
        long usuarioId = Long.parseLong(jwt.getSubject());
        var filme = filmes.findById(filmeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "filme não encontrado"));
        if (reviews.existsByUsuarioIdAndFilmeId(usuarioId, filmeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "você já avaliou este filme");
        }
        var review = reviews.save(new Review(usuarios.getReferenceById(usuarioId), filme, req.texto(),
                req.nota(), req.medometro(), LocalDateTime.now(clock)));
        return ReviewResponse.de(review);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Pagina<ReviewResponse> listar(@PathVariable long filmeId,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        var p = reviews.findByFilmeId(filmeId, PageRequest.of(page, size, Sort.by("data").descending().and(Sort.by("id").descending())));
        return new Pagina<>(p.map(ReviewResponse::de).getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
