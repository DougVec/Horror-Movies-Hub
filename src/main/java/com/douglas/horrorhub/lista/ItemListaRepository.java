package com.douglas.horrorhub.lista;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemListaRepository extends JpaRepository<ItemLista, Long> {

    @EntityGraph(attributePaths = "filme")
    List<ItemLista> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = "filme")
    List<ItemLista> findByUsuarioIdAndStatusOrderByIdDesc(Long usuarioId, ItemLista.Status status);

    @EntityGraph(attributePaths = "filme")
    Optional<ItemLista> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndFilmeId(Long usuarioId, Long filmeId);
}
