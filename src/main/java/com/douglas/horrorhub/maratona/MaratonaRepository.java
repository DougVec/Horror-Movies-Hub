package com.douglas.horrorhub.maratona;

import com.douglas.horrorhub.filme.Filme;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface MaratonaRepository extends JpaRepository<Maratona, Long> {

    @EntityGraph(attributePaths = "filmes")
    List<Maratona> findByUsuarioIdOrderByCriadaEmDesc(Long usuarioId, Pageable pageable);

    /** Projeção usada para pontuar cada filme candidato. */
    interface Estatistica {
        Filme getFilme();
        Double getNota();
        Double getMedometro();
    }

    @Query("""
            select f as filme, avg(r.nota) as nota, avg(r.medometro) as medometro
            from Filme f left join Review r on r.filme = f
            where f.id in :ids
            group by f""")
    List<Estatistica> estatisticas(Collection<Long> ids);

    @Query("""
            select i.filme.id from ItemLista i
            where i.usuario.id = :usuarioId and i.status = com.douglas.horrorhub.lista.ItemLista.Status.QUERO_VER""")
    List<Long> idsQueroVer(Long usuarioId);

    /** Hub inteiro, menos o que o usuário já viu; só filmes com duração conhecida. */
    @Query("""
            select f.id from Filme f
            where f.duracaoMin is not null and f.id not in (
                select i.filme.id from ItemLista i
                where i.usuario.id = :usuarioId and i.status = com.douglas.horrorhub.lista.ItemLista.Status.ASSISTIDO)""")
    List<Long> idsNaoAssistidos(Long usuarioId);
}
