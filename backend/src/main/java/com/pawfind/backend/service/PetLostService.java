
package com.pawfind.backend.service;

import com.pawfind.backend.dto.PetLostCreateDTO;
import com.pawfind.backend.vo.PetLostDetailVO;

public interface PetLostService {

    // 发布宠物走失信息
    PetLostDetailVO createLostPet(
            PetLostCreateDTO dto,
            Long currentUserId
    );

    // 根据走失记录 ID 查询详情
    PetLostDetailVO getLostPetById(Long id);
}
