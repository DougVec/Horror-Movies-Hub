package com.douglas.horrorhub.alerta;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    /** Já traz usuário e filme: o job roda fora de transação. */
    @Query("select a from Alerta a join fetch a.usuario join fetch a.filme where a.ativo order by a.id")
    List<Alerta> ativosComDetalhes();

    @EntityGraph(attributePaths = "filme")
    List<Alerta> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    boolean existsByUsuarioIdAndFilmeIdAndTipo(Long usuarioId, Long filmeId, Alerta.Tipo tipo);

    @Modifying
    @Query("delete from Alerta a where a.usuario.id = :usuarioId and a.filme.id = :filmeId and a.tipo in :tipos")
    int remover(Long usuarioId, Long filmeId, Collection<Alerta.Tipo> tipos);
}
