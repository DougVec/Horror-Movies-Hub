package com.douglas.horrorhub.lista;

import com.douglas.horrorhub.filme.FilmeController.FilmeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/minha-lista")
public class ListaController {

    public record AdicionarRequest(@NotNull Long filmeId, ItemLista.Status status) {}

    public record AtualizarRequest(ItemLista.Status status, @Min(0) @Max(10) Integer nota) {}

    public record ItemResponse(Long id, FilmeResponse filme, ItemLista.Status status, Integer nota, LocalDate assistidoEm) {
        static ItemResponse de(ItemLista i) {
            return new ItemResponse(i.getId(), FilmeResponse.de(i.getFilme()), i.getStatus(), i.getNota(), i.getAssistidoEm());
        }
    }

    private final ListaService service;

    public ListaController(ListaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemResponse> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) ItemLista.Status status) {
        return service.listar(usuarioId(jwt), status).stream().map(ItemResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse adicionar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AdicionarRequest req) {
        return ItemResponse.de(service.adicionar(usuarioId(jwt), req.filmeId(), req.status()));
    }

    @PatchMapping("/{id}")
    public ItemResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                  @Valid @RequestBody AtualizarRequest req) {
        return ItemResponse.de(service.atualizar(usuarioId(jwt), id, req.status(), req.nota()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        service.remover(usuarioId(jwt), id);
    }

    static long usuarioId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
