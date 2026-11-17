package com.mdb.backend.service;

import com.mdb.backend.dto.response.UserResponse;
import com.mdb.backend.entity.User;
import com.mdb.backend.exception.ResourceNotFoundException;
import com.mdb.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com id " + id + " não encontrado."));
    }

    public UserResponse findById(Long id) {
        User user = getOrThrow(id);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
