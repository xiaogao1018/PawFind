package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.PetProfileDTO;
import com.pawfind.backend.entity.*;
import com.pawfind.backend.mapper.*;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
public class PersonalCenterService {
    private final PetProfileMapper pets;
    private final PetLostMapper lost;
    private final PetFoundMapper found;
    private final CommunityPostMapper posts;

    public PersonalCenterService(PetProfileMapper pets, PetLostMapper lost, PetFoundMapper found,
                                 CommunityPostMapper posts) {
        this.pets = pets;
        this.lost = lost;
        this.found = found;
        this.posts = posts;
    }

    @Transactional(readOnly = true)
    public Summary summary(Long userId) {
        return new Summary(pets.selectCount(Wrappers.<PetProfile>lambdaQuery().eq(PetProfile::getUserId, userId)),
                lost.selectCount(Wrappers.<PetLost>lambdaQuery().eq(PetLost::getUserId, userId)),
                found.selectCount(Wrappers.<PetFound>lambdaQuery().eq(PetFound::getUserId, userId)),
                posts.selectCount(Wrappers.<CommunityPost>lambdaQuery().eq(CommunityPost::getUserId, userId)));
    }

    public PageVO<PetProfile> pets(Long userId, int page, int size) {
        return PageVO.query(pets, Wrappers.<PetProfile>lambdaQuery().eq(PetProfile::getUserId, userId),
                PetProfile::getId, page, size, Function.identity());
    }

    @Transactional
    public PetProfile savePet(Long id, Long userId, PetProfileDTO dto) {
        PetProfile pet = new PetProfile();
        if (id != null) {
            pet = pets.selectOne(Wrappers.<PetProfile>lambdaQuery().eq(PetProfile::getId, id)
                    .eq(PetProfile::getUserId, userId).last("FOR UPDATE"));
            if (pet == null) {
                throw new ResourceNotFoundException("宠物档案不存在");
            }
            pets.update(Wrappers.<PetProfile>lambdaUpdate().eq(PetProfile::getId, id)
                    .set(PetProfile::getName, dto.name().strip())
                    .set(PetProfile::getSpecies, dto.species().strip())
                    .set(PetProfile::getBreed, dto.breed())
                    .set(PetProfile::getBirthDate, dto.birthDate())
                    .set(PetProfile::getDescription, dto.description()));
            return pets.selectById(id);
        }
        pet.setUserId(userId);
        pet.setName(dto.name().strip());
        pet.setSpecies(dto.species().strip());
        pet.setBreed(dto.breed());
        pet.setBirthDate(dto.birthDate());
        pet.setDescription(dto.description());
        pet.setCreatedAt(LocalDateTime.now());
        pets.insert(pet);
        return pet;
    }

    @Transactional
    public void deletePet(Long id, Long userId) {
        int rows = pets.delete(Wrappers.<PetProfile>lambdaQuery()
                .eq(PetProfile::getId, id).eq(PetProfile::getUserId, userId));
        if (rows == 0) {
            throw new ResourceNotFoundException("宠物档案不存在");
        }
    }

    public PageVO<PetLost> lost(Long userId, int page, int size) {
        return PageVO.query(lost, Wrappers.<PetLost>lambdaQuery().eq(PetLost::getUserId, userId),
                PetLost::getId, page, size, Function.identity());
    }

    public PageVO<PetFound> found(Long userId, int page, int size) {
        return PageVO.query(found, Wrappers.<PetFound>lambdaQuery().eq(PetFound::getUserId, userId),
                PetFound::getId, page, size, Function.identity());
    }

    public record Summary(long pets, long lost, long found, long posts) {
    }
}
