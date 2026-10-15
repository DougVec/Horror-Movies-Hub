package com.douglas.horrorhub.filme;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface FilmeRepository extends JpaRepository<Filme, Long> {

    List<Filme> findByTmdbIdIn(Collection<Long> tmdbIds);

    interface Comunidade {
        Double getNota();
        Double getMedometro();
        long getTotal();
    }

    @Query("select avg(r.nota) as nota, avg(r.medometro) as medometro, count(r) as total from Review r where r.filme.id = :filmeId")
    Comunidade comunidade(Long filmeId);

    @Query("""
            select r.filme as filme, avg(r.nota) as nota, avg(r.medometro) as medometro, count(r) as total
            from Review r group by r.filme
            having count(r) >= :minimoReviews
            order by avg(r.nota) desc, count(r) desc""")
    List<Ranking> emAlta(long minimoReviews, Pageable pageable);

    interface Ranking extends Comunidade {
        Filme getFilme();
    }
}
