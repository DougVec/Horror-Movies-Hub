package com.douglas.horrorhub.filme;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    public record FilmeResponse(Long id, Long tmdbId, String titulo, String tituloOriginal, String sinopse,
                                Integer duracaoMin, LocalDate dataLancamento, String posterUrl, BigDecimal notaTmdb) {
        public static FilmeResponse de(Filme f) {
            return new FilmeResponse(f.getId(), f.getTmdbId(), f.getTitulo(), f.getTituloOriginal(), f.getSinopse(),
                    f.getDuracaoMin(), f.getDataLancamento(), f.getPosterUrl(), f.getNotaTmdb());
        }
    }

    public record Comunidade(Double nota, Double medometro, long totalReviews) {
        static Comunidade de(FilmeRepository.Comunidade c) {
            return new Comunidade(c.getNota(), c.getMedometro(), c.getTotal());
        }
    }

    public record DetalheResponse(FilmeResponse filme, Comunidade comunidade) {}

    private final FilmeService service;

    public FilmeController(FilmeService service) {
        this.service = service;
    }

    @GetMapping("/busca")
    public List<FilmeResponse> busca(@RequestParam @NotBlank @Size(max = 100) String q) {
        return service.buscar(q).stream().map(FilmeResponse::de).toList();
    }

    @GetMapping("/em-breve")
    public List<FilmeResponse> emBreve() {
        return service.emBreve().stream().map(FilmeResponse::de).toList();
    }

    @GetMapping("/em-alta")
    public List<DetalheResponse> emAlta() {
        return service.emAlta().stream()
                .map(r -> new DetalheResponse(FilmeResponse.de(r.getFilme()), Comunidade.de(r)))
                .toList();
    }

    @GetMapping("/{id}")
    public DetalheResponse detalhe(@PathVariable long id) {
        return new DetalheResponse(FilmeResponse.de(service.detalhe(id)), Comunidade.de(service.comunidade(id)));
    }
}
