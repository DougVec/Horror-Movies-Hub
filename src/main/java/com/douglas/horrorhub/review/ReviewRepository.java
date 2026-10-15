package com.douglas.horrorhub.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "usuario")
    Page<Review> findByFilmeId(Long filmeId, Pageable pageable);

    boolean existsByUsuarioIdAndFilmeId(Long usuarioId, Long filmeId);
}
