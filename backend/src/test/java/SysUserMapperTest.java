
package com.pawfind.backend;

import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SysUserMapperTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void testSelectUsers() {
        List<SysUser> users = sysUserMapper.selectList(null);

        assertNotNull(users);

        System.out.println("用户总数：" + users.size());
    }
}
