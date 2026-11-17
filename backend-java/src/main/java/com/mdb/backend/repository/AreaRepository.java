package com.mdb.backend.repository;

import com.mdb.backend.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {
    Optional<Area> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
