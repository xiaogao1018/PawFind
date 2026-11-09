
package com.pawfind.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;

import com.pawfind.backend.dto.PetLostCreateDTO;
import com.pawfind.backend.dto.PetLostUpdateDTO;

import com.pawfind.backend.entity.PetLost;
import com.pawfind.backend.entity.SysUser;

import com.pawfind.backend.mapper.PetLostMapper;
import com.pawfind.backend.mapper.SysUserMapper;

import com.pawfind.backend.service.PetLostService;

import com.pawfind.backend.vo.PetLostDetailVO;
import com.pawfind.backend.vo.PetLostPageVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    // =========================
    // W3D1：发布走失信息
    // =========================

    @Override
    @Transactional
    public PetLostDetailVO createLostPet(
            PetLostCreateDTO dto,
            Long currentUserId) {

        if (currentUserId == null || currentUserId <= 0) {
            throw new InvalidCredentialsException("请先登录");
        }

        SysUser currentUser =
                sysUserMapper.selectById(currentUserId);

        if (currentUser == null
                || !"ACTIVE".equals(currentUser.getStatus())) {
            throw new InvalidCredentialsException("当前账号不可用");
        }

        PetLost petLost = new PetLost();

        petLost.setUserId(currentUserId);
        petLost.setPetName(dto.getPetName());
        petLost.setSpecies(dto.getSpecies().trim());
        petLost.setDescription(dto.getDescription());
        petLost.setLostTime(dto.getLostTime());
        petLost.setLostLocation(dto.getLostLocation().trim());

        petLost.setStatus("LOOKING");

        LocalDateTime now = LocalDateTime.now();
        petLost.setCreatedAt(now);
        petLost.setUpdatedAt(now);

        petLostMapper.insert(petLost);

        return toDetailVO(petLost);
    }

    // =========================
    // W3D1：查询走失详情
    // =========================

    @Override
    public PetLostDetailVO getLostPetById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "走失记录ID必须大于0"
            );
        }

        PetLost petLost = petLostMapper.selectById(id);

        if (petLost == null
                || "WITHDRAWN".equals(petLost.getStatus())) {
            throw new ResourceNotFoundException(
                    "走失信息不存在或已撤回"
            );
        }

        return toDetailVO(petLost);
    }

    // =========================
    // W3D2：编辑走失信息
    // =========================

    @Override
    @Transactional
    public PetLostDetailVO updateLostPet(
            Long id,
            PetLostUpdateDTO dto,
            Long currentUserId) {

        PetLost petLost =
                getOwnedLookingPet(id, currentUserId);

        // 更新宠物名称，允许设置为空
        String petName = dto.getPetName();

        if (petName != null) {
            petName = petName.isBlank() ? null : petName.trim();
        }

        // 使用 UpdateWrapper，允许将可空字段更新为 NULL
        LambdaUpdateWrapper<PetLost> wrapper =
                new LambdaUpdateWrapper<>();

        wrapper.eq(PetLost::getId, id)
                .eq(PetLost::getUserId, currentUserId)
                .eq(PetLost::getStatus, "LOOKING")
                .set(PetLost::getPetName, petName)
                .set(PetLost::getSpecies, dto.getSpecies().trim())
                .set(PetLost::getDescription, dto.getDescription())
                .set(PetLost::getLostTime, dto.getLostTime())
                .set(PetLost::getLostLocation,
                        dto.getLostLocation().trim());

        int rows = petLostMapper.update(null, wrapper);

        if (rows == 0) {
            throw new IllegalArgumentException(
                    "走失信息状态已变化，无法编辑"
            );
        }

        return toDetailVO(petLostMapper.selectById(id));
    }

    // =========================
    // W3D2：撤回走失信息
    // =========================

    @Override
    @Transactional
    public PetLostDetailVO withdrawLostPet(
            Long id,
            Long currentUserId) {

        getOwnedLookingPet(id, currentUserId);

        PetLost update = new PetLost();
        update.setId(id);
        update.setStatus("WITHDRAWN");

        int rows = petLostMapper.updateById(update);

        if (rows == 0) {
            throw new IllegalArgumentException(
                    "走失信息状态已变化，无法撤回"
            );
        }

        return toDetailVO(petLostMapper.selectById(id));
    }

    // =========================
    // W3D2：标记已找到
    // =========================

    @Override
    @Transactional
    public PetLostDetailVO markAsFound(
            Long id,
            Long currentUserId) {

        getOwnedLookingPet(id, currentUserId);

        PetLost update = new PetLost();
        update.setId(id);
        update.setStatus("FOUND");

        int rows = petLostMapper.updateById(update);

        if (rows == 0) {
            throw new IllegalArgumentException(
                    "走失信息状态已变化，无法修改"
            );
        }

        return toDetailVO(petLostMapper.selectById(id));
    }

    // =========================
    // W3D2：分页查询
    // =========================

    @Override
    public PetLostPageVO getLostPetPage(
            Integer page,
            Integer size) {

        if (page == null || page < 1) {
            throw new IllegalArgumentException(
                    "页码必须大于或等于1"
            );
        }

        if (size == null || size < 1 || size > 50) {
            throw new IllegalArgumentException(
                    "每页数量必须在1到50之间"
            );
        }

        long offset = ((long) page - 1) * size;

        List<PetLost> petLostList =
                petLostMapper.selectVisiblePage(offset, size);

        long total = petLostMapper.countVisible();

        List<PetLostDetailVO> records = new ArrayList<>();

        for (PetLost petLost : petLostList) {
            records.add(toDetailVO(petLost));
        }

        PetLostPageVO pageVO = new PetLostPageVO();

        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setTotal(total);
        pageVO.setTotalPages((total + size - 1) / size);
        pageVO.setRecords(records);

        return pageVO;
    }

    // =========================
    // 公共方法：检查发布者权限和状态
    // =========================

    private PetLost getOwnedLookingPet(
            Long id,
            Long currentUserId) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "走失记录ID必须大于0"
            );
        }

        if (currentUserId == null || currentUserId <= 0) {
            throw new InvalidCredentialsException("请先登录");
        }

        PetLost petLost = petLostMapper.selectById(id);

        if (petLost == null) {
            throw new ResourceNotFoundException("走失信息不存在");
        }

        // 判断当前用户是不是发布者
        if (!Objects.equals(
                petLost.getUserId(), currentUserId)) {

            throw new ForbiddenException(
                    "无权操作其他用户发布的走失信息"
            );
        }

        // 只有 LOOKING 状态允许操作
        if (!"LOOKING".equals(petLost.getStatus())) {
            throw new IllegalArgumentException(
                    "只有正在寻找中的信息可以操作"
            );
        }

        return petLost;
    }

    // =========================
    // Entity 转换为 VO
    // =========================

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
