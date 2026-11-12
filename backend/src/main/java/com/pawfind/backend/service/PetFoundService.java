package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.PetFoundDTO;
import com.pawfind.backend.entity.PetFound;
import com.pawfind.backend.mapper.PetFoundMapper;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
public class PetFoundService {
    private final PetFoundMapper mapper;

    public PetFoundService(PetFoundMapper mapper) {
        this.mapper = mapper;
    }

    public PageVO<PetFound> list(int page, int size) {
        return PageVO.query(mapper, Wrappers.<PetFound>lambdaQuery().eq(PetFound::getStatus, "OPEN")
                .eq(PetFound::getVisibilityStatus, "VISIBLE"),
                PetFound::getId, page, size, Function.identity());
    }

    public PetFound get(long id) {
        return requireOpen(mapper.selectById(id));
    }

    @Transactional
    public PetFound create(PetFoundDTO dto, Long userId) {
        PetFound found = new PetFound();
        found.setUserId(userId);
        found.setPetName(cleanOptional(dto.petName()));
        found.setSpecies(species(dto));
        found.setDescription(cleanOptional(dto.description()));
        found.setFoundTime(dto.foundTime());
        found.setFoundLocation(dto.foundLocation().strip());
        found.setStatus("OPEN");
        found.setVisibilityStatus("VISIBLE");
        found.setCreatedAt(LocalDateTime.now());
        mapper.insert(found);
        return found;
    }

    @Transactional
    public PetFound update(long id, PetFoundDTO dto, Long userId) {
        lockOwnedRecord(id, userId);
        mapper.update(Wrappers.<PetFound>lambdaUpdate().eq(PetFound::getId, id)
                .set(PetFound::getPetName, cleanOptional(dto.petName()))
                .set(PetFound::getSpecies, species(dto))
                .set(PetFound::getDescription, cleanOptional(dto.description()))
                .set(PetFound::getFoundTime, dto.foundTime())
                .set(PetFound::getFoundLocation, dto.foundLocation().strip()));
        return mapper.selectById(id);
    }

    @Transactional
    public void withdraw(long id, Long userId) {
        lockOwnedRecord(id, userId);
        mapper.update(Wrappers.<PetFound>lambdaUpdate().eq(PetFound::getId, id)
                .set(PetFound::getStatus, "WITHDRAWN"));
    }

    private void lockOwnedRecord(long id, Long userId) {
        PetFound found = requireOpen(mapper.selectOne(Wrappers.<PetFound>lambdaQuery()
                .eq(PetFound::getId, id).last("FOR UPDATE")));
        if (!userId.equals(found.getUserId())) {
            throw new ForbiddenException("只能操作自己发布的发现记录");
        }
    }

    private PetFound requireOpen(PetFound found) {
        if (found == null || !"OPEN".equals(found.getStatus())
                || !"VISIBLE".equals(found.getVisibilityStatus())) {
            throw new ResourceNotFoundException("发现记录不存在或已撤回");
        }
        return found;
    }

    private String species(PetFoundDTO dto) {
        return dto.species() == null || dto.species().isBlank() ? "未知" : dto.species().strip();
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
