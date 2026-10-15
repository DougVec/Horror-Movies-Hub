package com.douglas.horrorhub.lista;

import com.douglas.horrorhub.auth.UsuarioRepository;
import com.douglas.horrorhub.filme.FilmeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class ListaService {

    private final ItemListaRepository itens;
    private final FilmeRepository filmes;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ListaService(ItemListaRepository itens, FilmeRepository filmes, UsuarioRepository usuarios, Clock clock) {
        this.itens = itens;
        this.filmes = filmes;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ItemLista> listar(long usuarioId, ItemLista.Status status) {
        return status == null
                ? itens.findByUsuarioIdOrderByIdDesc(usuarioId)
                : itens.findByUsuarioIdAndStatusOrderByIdDesc(usuarioId, status);
    }

    @Transactional
    public ItemLista adicionar(long usuarioId, long filmeId, ItemLista.Status status) {
        var filme = filmes.findById(filmeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "filme não encontrado"));
        if (itens.existsByUsuarioIdAndFilmeId(usuarioId, filmeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "filme já está na sua lista");
        }
        return itens.save(new ItemLista(usuarios.getReferenceById(usuarioId), filme,
                status == null ? ItemLista.Status.QUERO_VER : status, LocalDate.now(clock)));
    }

    /** status e nota são opcionais; status é aplicado antes para permitir "ASSISTIDO + nota" numa chamada só. */
    @Transactional
    public ItemLista atualizar(long usuarioId, long itemId, ItemLista.Status status, Integer nota) {
        var item = doUsuario(usuarioId, itemId);
        if (status != null) item.mudarStatus(status, LocalDate.now(clock));
        if (nota != null) {
            try {
                item.avaliar(nota);
            } catch (IllegalStateException | IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
        }
        return item;
    }

    @Transactional
    public void remover(long usuarioId, long itemId) {
        itens.delete(doUsuario(usuarioId, itemId));
    }

    /** Item de outro usuário responde 404, igual a inexistente, para não revelar ids alheios. */
    private ItemLista doUsuario(long usuarioId, long itemId) {
        return itens.findByIdAndUsuarioId(itemId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item não encontrado"));
    }
}
