package com.mdb.backend.service;

import com.mdb.backend.dto.response.DifficultyRatingResponse;
import com.mdb.backend.entity.DifficultyRating;
import com.mdb.backend.entity.Problem;
import com.mdb.backend.entity.User;
import com.mdb.backend.exception.DuplicateResourceException;
import com.mdb.backend.exception.ResourceNotFoundException;
import com.mdb.backend.repository.DifficultyRatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DifficultyRatingService {

    private final DifficultyRatingRepository difficultyRatingRepository;
    private final ProblemService problemService;

    public DifficultyRatingService(DifficultyRatingRepository difficultyRatingRepository, ProblemService problemService) {
        this.difficultyRatingRepository = difficultyRatingRepository;
        this.problemService = problemService;
    }

    public DifficultyRatingResponse get(Long problemId, Long userId) {
        problemService.getOrThrow(problemId);
        Double avg = difficultyRatingRepository.averageDifficultyForProblem(problemId);
        long count = difficultyRatingRepository.countForProblem(problemId);
        Integer userDifficulty = userId == null ? null :
                difficultyRatingRepository.findByUserIdAndProblemId(userId, problemId)
                        .map(DifficultyRating::getDifficulty).orElse(null);
        Double rounded = avg == null ? null : Math.round(avg * 10.0) / 10.0;
        return new DifficultyRatingResponse(problemId, rounded, count, userDifficulty);
    }

    @Transactional
    public DifficultyRatingResponse create(Long problemId, User user, int difficulty) {
        if (difficultyRatingRepository.findByUserIdAndProblemId(user.getId(), problemId).isPresent()) {
            throw new DuplicateResourceException(
                    "Você já avaliou este problema. Use PUT para alterar sua avaliação.");
        }
        Problem problem = problemService.getOrThrow(problemId);
        difficultyRatingRepository.save(DifficultyRating.builder()
                .user(user)
                .problem(problem)
                .difficulty(difficulty)
                .build());
        return get(problemId, user.getId());
    }

    @Transactional
    public DifficultyRatingResponse update(Long problemId, User user, int difficulty) {
        DifficultyRating rating = difficultyRatingRepository.findByUserIdAndProblemId(user.getId(), problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Você ainda não avaliou este problema. Use POST para criar uma avaliação."));
        rating.setDifficulty(difficulty);
        difficultyRatingRepository.save(rating);
        return get(problemId, user.getId());
    }
}
