package com.mdb.backend.repository;

import com.mdb.backend.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // dificuldade = média das avaliações ou, sem avaliações, a dificuldade inicial
    @Query("""
            SELECT DISTINCT p FROM Problem p
            LEFT JOIN p.tags t
            WHERE (:text = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :text, '%'))
                   OR LOWER(p.statement) LIKE LOWER(CONCAT('%', :text, '%')))
              AND (:areaId IS NULL OR p.area.id = :areaId)
              AND (:tagId IS NULL OR t.id = :tagId)
              AND COALESCE(
                    (SELECT AVG(dr.difficulty) FROM DifficultyRating dr WHERE dr.problem = p),
                    p.initialDifficulty
                  ) >= :minDifficulty
              AND COALESCE(
                    (SELECT AVG(dr.difficulty) FROM DifficultyRating dr WHERE dr.problem = p),
                    p.initialDifficulty
                  ) <= :maxDifficulty
            """)
    Page<Problem> search(
            @Param("text") String text,
            @Param("areaId") Long areaId,
            @Param("tagId") Long tagId,
            @Param("minDifficulty") double minDifficulty,
            @Param("maxDifficulty") double maxDifficulty,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT p FROM Problem p
            LEFT JOIN p.tags t
            WHERE (:areaId IS NULL OR p.area.id = :areaId)
              AND (:tagId IS NULL OR t.id = :tagId)
              AND COALESCE(
                    (SELECT AVG(dr.difficulty) FROM DifficultyRating dr WHERE dr.problem = p),
                    p.initialDifficulty
                  ) >= :minDifficulty
              AND COALESCE(
                    (SELECT AVG(dr.difficulty) FROM DifficultyRating dr WHERE dr.problem = p),
                    p.initialDifficulty
                  ) <= :maxDifficulty
            """)
    List<Problem> findAllMatching(
            @Param("areaId") Long areaId,
            @Param("tagId") Long tagId,
            @Param("minDifficulty") double minDifficulty,
            @Param("maxDifficulty") double maxDifficulty
    );
}
