package com.pawfind.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawfind.backend.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {
}
