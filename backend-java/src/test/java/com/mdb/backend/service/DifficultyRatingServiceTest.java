package com.mdb.backend.service;

import com.mdb.backend.entity.*;
import com.mdb.backend.exception.DuplicateResourceException;
import com.mdb.backend.exception.ResourceNotFoundException;
import com.mdb.backend.repository.DifficultyRatingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DifficultyRatingServiceTest {

    @Mock
    private DifficultyRatingRepository difficultyRatingRepository;

    @Mock
    private ProblemService problemService;

    @InjectMocks
    private DifficultyRatingService difficultyRatingService;

    private User user;
    private Problem problem;

    private void setupUserAndProblem() {
        user = User.builder().id(2L).name("Demo").email("demo@mdb.dev").role(Role.USER).build();
        Area area = Area.builder().id(1L).name("Algebra").build();
        problem = Problem.builder().id(10L).title("Test").area(area).initialDifficulty(2).build();
    }

    @Test
    void create_shouldRejectDuplicateRatingForSameUserAndProblem() {
        setupUserAndProblem();
        when(difficultyRatingRepository.findByUserIdAndProblemId(2L, 10L))
                .thenReturn(Optional.of(DifficultyRating.builder().id(1L).difficulty(3).build()));

        assertThatThrownBy(() -> difficultyRatingService.create(10L, user, 4))
                .isInstanceOf(DuplicateResourceException.class);

        verify(difficultyRatingRepository, never()).save(any());
    }

    @Test
    void create_shouldPersistFirstRating() {
        setupUserAndProblem();
        when(difficultyRatingRepository.findByUserIdAndProblemId(2L, 10L)).thenReturn(Optional.empty());
        when(problemService.getOrThrow(10L)).thenReturn(problem);
        when(difficultyRatingRepository.averageDifficultyForProblem(10L)).thenReturn(4.0);
        when(difficultyRatingRepository.countForProblem(10L)).thenReturn(1L);

        var response = difficultyRatingService.create(10L, user, 4);

        assertThat(response.communityDifficulty()).isEqualTo(4.0);
        verify(difficultyRatingRepository).save(any(DifficultyRating.class));
    }

    @Test
    void update_shouldFailWhenNoPreviousRatingExists() {
        setupUserAndProblem();
        when(difficultyRatingRepository.findByUserIdAndProblemId(2L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> difficultyRatingService.update(10L, user, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void get_shouldReturnNullCommunityDifficultyWhenNoRatingsYet() {
        setupUserAndProblem();
        when(difficultyRatingRepository.averageDifficultyForProblem(10L)).thenReturn(null);
        when(difficultyRatingRepository.countForProblem(10L)).thenReturn(0L);
        when(difficultyRatingRepository.findByUserIdAndProblemId(anyLong(), eq(10L))).thenReturn(Optional.empty());

        var response = difficultyRatingService.get(10L, 2L);

        assertThat(response.communityDifficulty()).isNull();
        assertThat(response.ratingsCount()).isZero();
    }
}
