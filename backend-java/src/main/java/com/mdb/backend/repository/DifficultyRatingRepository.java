package com.mdb.backend.repository;

import com.mdb.backend.entity.DifficultyRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DifficultyRatingRepository extends JpaRepository<DifficultyRating, Long> {

    Optional<DifficultyRating> findByUserIdAndProblemId(Long userId, Long problemId);

    @Query("SELECT AVG(dr.difficulty) FROM DifficultyRating dr WHERE dr.problem.id = :problemId")
    Double averageDifficultyForProblem(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(dr) FROM DifficultyRating dr WHERE dr.problem.id = :problemId")
    long countForProblem(@Param("problemId") Long problemId);
}
