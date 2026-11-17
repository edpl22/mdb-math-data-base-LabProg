package com.mdb.backend.service;

import com.mdb.backend.dto.request.AreaRequest;
import com.mdb.backend.dto.response.AreaResponse;
import com.mdb.backend.entity.Area;
import com.mdb.backend.exception.DuplicateResourceException;
import com.mdb.backend.repository.AreaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepository areaRepository;

    public AreaService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    public List<AreaResponse> findAll() {
        return areaRepository.findAll().stream()
                .map(a -> new AreaResponse(a.getId(), a.getName()))
                .toList();
    }

    @Transactional
    public AreaResponse create(AreaRequest request) {
        String name = request.name().trim();
        if (areaRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Já existe uma área chamada '" + name + "'.");
        }
        Area saved = areaRepository.save(Area.builder().name(name).build());
        return new AreaResponse(saved.getId(), saved.getName());
    }

    public Area findByNameOrThrow(String name) {
        return areaRepository.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new com.mdb.backend.exception.ResourceNotFoundException(
                        "Área '" + name + "' não encontrada. Cadastre-a antes via /api/areas."));
    }
}
