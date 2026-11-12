package com.pawfind.backend;

import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.CommunityPostDTO;
import com.pawfind.backend.dto.PetFoundDTO;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.service.AdminService;
import com.pawfind.backend.service.CommunityService;
import com.pawfind.backend.service.PetFoundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BusinessFlowTests {
    @Autowired
    CommunityService community;
    @Autowired
    AdminService admin;
    @Autowired
    PetFoundService foundPets;
    @Autowired
    SysUserMapper users;

    private Long ownerId;
    private Long readerId;
    private Long adminId;

    @BeforeEach
    void createUsers() {
        ownerId = createUser("USER");
        readerId = createUser("USER");
        adminId = createUser("ADMIN");
    }

    @Test
    void editingApprovedPostRemovesItFromPublicAccessUntilReviewedAgain() {
        var post = community.create(new CommunityPostDTO("寻宠", "原内容", null), ownerId);
        assertEquals(ownerId, post.getUserId());
        assertThrows(ResourceNotFoundException.class, () -> community.get(post.getId(), readerId, "USER"));
        assertThrows(ResourceNotFoundException.class, () -> community.comment(post.getId(), ownerId, "提前评论"));

        admin.audit(post.getId(), "APPROVED", null, adminId);
        assertEquals("APPROVED", community.get(post.getId(), readerId, "USER").getAuditStatus());
        community.update(post.getId(), new CommunityPostDTO("更新标题", "新内容", null), ownerId);
        assertThrows(ResourceNotFoundException.class, () -> community.get(post.getId(), readerId, "USER"));
        assertEquals("PENDING", community.get(post.getId(), ownerId, "USER").getAuditStatus());
        assertEquals(post.getId(), community.listMine(ownerId, 1, 10).records().getFirst().getId());
    }

    @Test
    void hidingPostBlocksCommentsAndWritesAnAuditLog() {
        var post = community.create(new CommunityPostDTO("寻宠", "正文", null), ownerId);
        admin.audit(post.getId(), "APPROVED", null, adminId);
        community.comment(post.getId(), readerId, "评论");
        assertThrows(IllegalArgumentException.class,
                () -> admin.setVisibility(post.getId(), "HIDDEN", " ", adminId));
        admin.setVisibility(post.getId(), "HIDDEN", "需要核实", adminId);
        assertThrows(ResourceNotFoundException.class, () -> community.get(post.getId(), readerId, "USER"));
        assertThrows(ResourceNotFoundException.class,
                () -> community.listComments(post.getId(), readerId, "USER"));
        var log = admin.listLogs(1, 1).records().getFirst();
        assertEquals("HIDE", log.getOperation());
        assertEquals("需要核实", log.getDetail());
        admin.setVisibility(post.getId(), "VISIBLE", null, adminId);
        assertEquals(1, community.listComments(post.getId(), readerId, "USER").size());
    }

    @Test
    void userPaginationCountsRowsWhenSelectingSafeUserFields() {
        var page = admin.listUsers(1, 50);
        assertTrue(page.total() >= 3);
        assertFalse(page.records().isEmpty());
        assertThrows(ForbiddenException.class, () -> admin.updateUserStatus(adminId, "DISABLED", adminId));
        admin.updateUserStatus(readerId, "DISABLED", adminId);
        assertEquals("DISABLED", users.selectById(readerId).getStatus());
    }

    @Test
    void foundPetSupportsNullableEditsAndOwnerOnlyWithdrawal() {
        var dto = new PetFoundDTO("团子", "猫", "橘色", LocalDateTime.now().minusDays(1), "公园附近");
        var found = foundPets.create(dto, ownerId);
        assertThrows(ForbiddenException.class, () -> foundPets.withdraw(found.getId(), readerId));
        var updated = foundPets.update(found.getId(),
                new PetFoundDTO(null, null, null, dto.foundTime(), dto.foundLocation()), ownerId);
        assertNull(updated.getPetName());
        assertNull(updated.getDescription());
        assertEquals("未知", updated.getSpecies());
        foundPets.withdraw(found.getId(), ownerId);
        assertThrows(ResourceNotFoundException.class, () -> foundPets.get(found.getId()));
    }

    private Long createUser(String role) {
        SysUser user = new SysUser();
        user.setUsername("test_" + UUID.randomUUID());
        user.setPasswordHash("unused-test-hash");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        users.insert(user);
        return user.getId();
    }
}
