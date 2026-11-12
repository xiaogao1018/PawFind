package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.PetFound;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PetFoundMapper extends BaseMapper<PetFound> {
}
