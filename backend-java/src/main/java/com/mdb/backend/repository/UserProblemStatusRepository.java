package com.mdb.backend.repository;

import com.mdb.backend.entity.StatusType;
import com.mdb.backend.entity.UserProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProblemStatusRepository extends JpaRepository<UserProblemStatus, Long> {

    Optional<UserProblemStatus> findByUserIdAndProblemIdAndStatus(Long userId, Long problemId, StatusType status);

    List<UserProblemStatus> findByUserIdAndProblemId(Long userId, Long problemId);

    List<UserProblemStatus> findByUserId(Long userId);

    List<UserProblemStatus> findByUserIdAndStatus(Long userId, StatusType status);
}
