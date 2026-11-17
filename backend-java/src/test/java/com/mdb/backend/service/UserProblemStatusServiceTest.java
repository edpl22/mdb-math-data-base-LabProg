package com.mdb.backend.service;

import com.mdb.backend.entity.*;
import com.mdb.backend.mapper.ProblemMapper;
import com.mdb.backend.repository.UserProblemStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProblemStatusServiceTest {

    @Mock private UserProblemStatusRepository userProblemStatusRepository;
    @Mock private ProblemService problemService;
    @Mock private ProblemMapper problemMapper;

    @InjectMocks
    private UserProblemStatusService userProblemStatusService;

    @Test
    void setStatus_shouldNotDuplicateWhenAlreadyMarked() {
        User user = User.builder().id(2L).name("Demo").role(Role.USER).build();
        Problem problem = Problem.builder().id(10L).build();
        when(problemService.getOrThrow(10L)).thenReturn(problem);
        when(userProblemStatusRepository.findByUserIdAndProblemIdAndStatus(2L, 10L, StatusType.FAVORITE))
                .thenReturn(Optional.of(UserProblemStatus.builder().id(1L).status(StatusType.FAVORITE).build()));
        when(userProblemStatusRepository.findByUserIdAndProblemId(2L, 10L)).thenReturn(List.of());

        userProblemStatusService.setStatus(10L, user, StatusType.FAVORITE);

        verify(userProblemStatusRepository, never()).save(any());
    }

    @Test
    void setStatus_shouldPersistWhenNotYetMarked() {
        User user = User.builder().id(2L).name("Demo").role(Role.USER).build();
        Problem problem = Problem.builder().id(10L).build();
        when(problemService.getOrThrow(10L)).thenReturn(problem);
        when(userProblemStatusRepository.findByUserIdAndProblemIdAndStatus(2L, 10L, StatusType.WANT_TO_DO))
                .thenReturn(Optional.empty());
        when(userProblemStatusRepository.findByUserIdAndProblemId(2L, 10L)).thenReturn(List.of());

        userProblemStatusService.setStatus(10L, user, StatusType.WANT_TO_DO);

        verify(userProblemStatusRepository).save(any(UserProblemStatus.class));
    }
}
