package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.PetProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PetProfileMapper extends BaseMapper<PetProfile> {
}
