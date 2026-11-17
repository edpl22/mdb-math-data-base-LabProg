package com.mdb.backend.service;

import com.mdb.backend.dto.response.ProblemSummaryResponse;
import com.mdb.backend.dto.response.UserStatusResponse;
import com.mdb.backend.entity.Problem;
import com.mdb.backend.entity.StatusType;
import com.mdb.backend.entity.User;
import com.mdb.backend.entity.UserProblemStatus;
import com.mdb.backend.mapper.ProblemMapper;
import com.mdb.backend.repository.UserProblemStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserProblemStatusService {

    private final UserProblemStatusRepository userProblemStatusRepository;
    private final ProblemService problemService;
    private final ProblemMapper problemMapper;

    public UserProblemStatusService(
            UserProblemStatusRepository userProblemStatusRepository,
            ProblemService problemService,
            ProblemMapper problemMapper
    ) {
        this.userProblemStatusRepository = userProblemStatusRepository;
        this.problemService = problemService;
        this.problemMapper = problemMapper;
    }

    @Transactional
    public UserStatusResponse setStatus(Long problemId, User user, StatusType status) {
        Problem problem = problemService.getOrThrow(problemId);
        boolean exists = userProblemStatusRepository
                .findByUserIdAndProblemIdAndStatus(user.getId(), problemId, status).isPresent();
        if (!exists) {
            userProblemStatusRepository.save(UserProblemStatus.builder()
                    .user(user)
                    .problem(problem)
                    .status(status)
                    .build());
        }
        return currentStatuses(user.getId(), problemId);
    }

    @Transactional
    public UserStatusResponse removeStatus(Long problemId, User user, StatusType status) {
        userProblemStatusRepository.findByUserIdAndProblemIdAndStatus(user.getId(), problemId, status)
                .ifPresent(userProblemStatusRepository::delete);
        return currentStatuses(user.getId(), problemId);
    }

    public UserStatusResponse currentStatuses(Long userId, Long problemId) {
        List<String> statuses = userProblemStatusRepository.findByUserIdAndProblemId(userId, problemId).stream()
                .map(s -> s.getStatus().name())
                .sorted()
                .toList();
        return new UserStatusResponse(statuses);
    }

    public List<ProblemSummaryResponse> findUserProblemsByStatus(Long userId, StatusType status) {
        return userProblemStatusRepository.findByUserIdAndStatus(userId, status).stream()
                .map(UserProblemStatus::getProblem)
                .sorted(Comparator.comparing(Problem::getUpdatedAt).reversed())
                .map(problemMapper::toSummary)
                .toList();
    }
}
