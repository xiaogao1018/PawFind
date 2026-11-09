
package com.pawfind.backend.service.impl;

import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.PetLostCreateDTO;
import com.pawfind.backend.entity.PetLost;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.PetLostMapper;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.service.PetLostService;
import com.pawfind.backend.vo.PetLostDetailVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PetLostServiceImpl implements PetLostService {

    private final PetLostMapper petLostMapper;
    private final SysUserMapper sysUserMapper;

    public PetLostServiceImpl(
            PetLostMapper petLostMapper,
            SysUserMapper sysUserMapper) {

        this.petLostMapper = petLostMapper;
        this.sysUserMapper = sysUserMapper;
    }

    // 发布走失信息
    @Override
    @Transactional
    public PetLostDetailVO createLostPet(
            PetLostCreateDTO dto,
            Long currentUserId) {

        // 检查登录身份
        if (currentUserId == null || currentUserId <= 0) {
            throw new InvalidCredentialsException("请先登录");
        }

        // 检查发布者是否存在且账号正常
        SysUser currentUser =
                sysUserMapper.selectById(currentUserId);

        if (currentUser == null
                || !"ACTIVE".equals(currentUser.getStatus())) {
            throw new InvalidCredentialsException("当前账号不可用");
        }

        // DTO 转换为 Entity
        PetLost petLost = new PetLost();

        petLost.setUserId(currentUserId);
        petLost.setPetName(dto.getPetName());
        petLost.setSpecies(dto.getSpecies().trim());
        petLost.setDescription(dto.getDescription());

        petLost.setLostTime(dto.getLostTime());
        petLost.setLostLocation(
                dto.getLostLocation().trim()
        );

        // 默认寻找中
        petLost.setStatus("LOOKING");

        LocalDateTime now = LocalDateTime.now();
        petLost.setCreatedAt(now);
        petLost.setUpdatedAt(now);

        // 保存到数据库
        petLostMapper.insert(petLost);

        // 返回详情
        return toDetailVO(petLost);
    }

    // 根据 ID 查询走失信息
    @Override
    public PetLostDetailVO getLostPetById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "走失记录ID必须大于0"
            );
        }

        PetLost petLost = petLostMapper.selectById(id);

        if (petLost == null) {
            throw new ResourceNotFoundException(
                    "走失记录不存在"
            );
        }

        return toDetailVO(petLost);
    }

    // Entity 转换为 VO
    private PetLostDetailVO toDetailVO(PetLost petLost) {

        PetLostDetailVO vo = new PetLostDetailVO();

        vo.setId(petLost.getId());
        vo.setUserId(petLost.getUserId());

        vo.setPetName(petLost.getPetName());
        vo.setSpecies(petLost.getSpecies());
        vo.setDescription(petLost.getDescription());

        vo.setLostTime(petLost.getLostTime());
        vo.setLostLocation(petLost.getLostLocation());

        vo.setStatus(petLost.getStatus());
        vo.setCreatedAt(petLost.getCreatedAt());

        return vo;
    }
}
