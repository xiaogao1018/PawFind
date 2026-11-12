package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.entity.PetClue;
import com.pawfind.backend.entity.PetLost;
import com.pawfind.backend.mapper.PetClueMapper;
import com.pawfind.backend.mapper.PetLostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetClueService {
    private final PetClueMapper clues;
    private final PetLostMapper lostPets;
    private final NotificationService notices;

    public PetClueService(PetClueMapper clues, PetLostMapper lostPets, NotificationService notices) {
        this.clues = clues;
        this.lostPets = lostPets;
        this.notices = notices;
    }

    public List<PetClue> list(long lostId, Long userId, String role) {
        PetLost lost = requireLost(lostPets.selectById(lostId));
        if (userId == null) {
            return List.of();
        }

        boolean canSeeAll = userId.equals(lost.getUserId()) || "ADMIN".equals(role);
        return clues.selectList(Wrappers.<PetClue>lambdaQuery()
                .eq(PetClue::getPetLostId, lostId)
                .eq(!canSeeAll, PetClue::getUserId, userId)
                .orderByDesc(PetClue::getId));
    }

    @Transactional
    public PetClue create(long lostId, String content, String contactInfo, LocalDateTime sightingTime,
                          String sightingLocation, Long userId) {
        PetLost lost = requireLost(lostPets.selectOne(Wrappers.<PetLost>lambdaQuery()
                .eq(PetLost::getId, lostId).last("FOR UPDATE")));
        if (!"LOOKING".equals(lost.getStatus())) {
            throw new IllegalArgumentException("只有寻找中的记录可以提交线索");
        }

        PetClue clue = new PetClue();
        clue.setPetLostId(lostId);
        clue.setUserId(userId);
        clue.setContent(content.strip());
        clue.setContactInfo(cleanOptional(contactInfo));
        clue.setSightingTime(sightingTime);
        clue.setSightingLocation(cleanOptional(sightingLocation));
        clue.setStatus("PENDING");
        clue.setCreatedAt(LocalDateTime.now());
        clues.insert(clue);
        notices.notify(lost.getUserId(), userId, "CLUE", "LOST", lostId, content.strip());
        return clue;
    }

    @Transactional
    public PetClue updateStatus(long id, String status, Long userId) {
        PetClue clue = clues.selectById(id);
        if (clue == null) {
            throw new ResourceNotFoundException("线索不存在");
        }

        PetLost lost = requireLost(lostPets.selectOne(Wrappers.<PetLost>lambdaQuery()
                .eq(PetLost::getId, clue.getPetLostId()).last("FOR UPDATE")));
        if (!userId.equals(lost.getUserId())) {
            throw new ForbiddenException("只有走失记录发布者可以核实线索");
        }

        clues.update(Wrappers.<PetClue>lambdaUpdate().eq(PetClue::getId, id)
                .set(PetClue::getStatus, status));
        clue.setStatus(status);
        return clue;
    }

    private PetLost requireLost(PetLost lost) {
        if (lost == null || "WITHDRAWN".equals(lost.getStatus())
                || !"VISIBLE".equals(lost.getVisibilityStatus())) {
            throw new ResourceNotFoundException("走失信息不存在或已撤回");
        }
        return lost;
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
