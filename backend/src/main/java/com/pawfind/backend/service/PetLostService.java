
package com.pawfind.backend.service;

import com.pawfind.backend.dto.PetLostCreateDTO;
import com.pawfind.backend.dto.PetLostUpdateDTO;
import com.pawfind.backend.vo.PetLostDetailVO;
import com.pawfind.backend.vo.PetLostPageVO;

public interface PetLostService {

    // 发布走失信息
    PetLostDetailVO createLostPet(
            PetLostCreateDTO dto,
            Long currentUserId
    );

    // 根据 ID 查询详情
    PetLostDetailVO getLostPetById(Long id);

    // 编辑走失信息
    PetLostDetailVO updateLostPet(
            Long id,
            PetLostUpdateDTO dto,
            Long currentUserId
    );

    // 撤回走失信息
    PetLostDetailVO withdrawLostPet(
            Long id,
            Long currentUserId
    );

    // 标记宠物已找到
    PetLostDetailVO markAsFound(
            Long id,
            Long currentUserId
    );

    // 分页查询走失信息
    PetLostPageVO getLostPetPage(
            Integer page,
            Integer size
    );
}
