package com.pawfind.backend;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.CommunityPostDTO;
import com.pawfind.backend.dto.PetFoundDTO;
import com.pawfind.backend.entity.CommunityComment;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.CommunityCommentMapper;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.service.*;
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
class ManagementAndMessageTests {
    @Autowired PersonalCenterService personal;
    @Autowired PetFoundService found;
    @Autowired AdminService admin;
    @Autowired CommunityService community;
    @Autowired RecordCommentService comments;
    @Autowired NotificationService notices;
    @Autowired DirectMessageService direct;
    @Autowired SysUserMapper users;
    @Autowired CommunityCommentMapper communityComments;

    private Long owner;
    private Long visitor;
    private Long outsider;
    private Long moderator;

    @BeforeEach
    void createUsers() {
        owner = user("USER");
        visitor = user("USER");
        outsider = user("USER");
        moderator = user("ADMIN");
    }

    @Test
    void recordCommentsNotifyOnlyTheOwnerAndHiddenRecordsCloseTheCommentEntry() {
        var pet = found.create(new PetFoundDTO("团子", "猫", null,
                LocalDateTime.now().minusDays(1), "社区花园"), owner);
        comments.create(false, pet.getId(), visitor, "我可能认识它");
        comments.create(false, pet.getId(), owner, "谢谢");
        assertEquals(1, notices.list(owner, 1, 20).total());
        assertEquals(0, notices.list(outsider, 1, 20).total());
        var notification = notices.list(owner, 1, 20).records().getFirst();
        notices.read(outsider, notification.id());
        assertEquals(1, notices.unread(owner).notifications());

        admin.recordVisibility(false, pet.getId(), "HIDDEN", "需要核实", moderator);
        assertThrows(ResourceNotFoundException.class, () -> found.get(pet.getId()));
        assertThrows(ResourceNotFoundException.class, () -> comments.list(false, pet.getId(), 1, 10));
        assertThrows(ResourceNotFoundException.class, () -> comments.create(false, pet.getId(), visitor, "新留言"));
        assertEquals("HIDDEN", personal.found(owner, 1, 10).records().getFirst().getVisibilityStatus());

        admin.recordVisibility(false, pet.getId(), "VISIBLE", null, moderator);
        assertEquals("OPEN", found.get(pet.getId()).getStatus());
        assertEquals(2, comments.list(false, pet.getId(), 1, 10).total());
        assertEquals("RESTORE", admin.listLogs(1, 10).records().getFirst().getOperation());
    }

    @Test
    void directMessagesStayWithinThePairAndReadMarkersPreserveNewerUnreadMessages() {
        var first = direct.send(owner, visitor, "你好");
        var second = direct.send(owner, visitor, "想了解一下宠物的情况");
        assertEquals(2, direct.thread(visitor, owner, null, null, 30).records().size());
        assertEquals(0, direct.thread(outsider, owner, null, null, 30).records().size());
        direct.read(outsider, owner, second.getId());
        assertEquals(2, notices.unread(visitor).direct());

        direct.read(visitor, owner, first.getId());
        assertEquals(1, notices.unread(visitor).direct());
        assertEquals(1, direct.conversations(visitor, 1, 20).records().getFirst().unreadCount());
        var older = direct.thread(visitor, owner, second.getId(), null, 1);
        assertEquals(first.getId(), older.records().getFirst().getId());
        var newer = direct.thread(visitor, owner, null, first.getId(), 1);
        assertEquals(second.getId(), newer.records().getFirst().getId());
        assertThrows(IllegalArgumentException.class, () -> direct.send(owner, owner, "自己"));

        users.update(Wrappers.<SysUser>lambdaUpdate().eq(SysUser::getId, visitor).set(SysUser::getStatus, "DISABLED"));
        assertThrows(ForbiddenException.class, () -> direct.send(owner, visitor, "再次发送"));
    }

    @Test
    void moderationRemovesCommentsFromPublicViewsAndLogsEachRealTransition() {
        var post = community.create(new CommunityPostDTO("分享", "正文", "EXPERIENCE"), owner);
        admin.audit(post.getId(), "APPROVED", null, moderator);
        var comment = community.comment(post.getId(), visitor, "很有帮助");
        assertEquals(1, notices.list(owner, 1, 20).records().stream()
                .filter(item -> "COMMENT".equals(item.type())).count());
        admin.commentVisibility(comment.getId(), "HIDDEN", "包含不当内容", moderator);
        assertTrue(community.listComments(post.getId(), owner, "USER").isEmpty());
        assertEquals("HIDDEN", communityComments.selectById(comment.getId()).getVisibilityStatus());
        admin.commentVisibility(comment.getId(), "VISIBLE", null, moderator);
        assertEquals(1, community.listComments(post.getId(), owner, "USER").size());
        community.deleteComment(post.getId(), comment.getId(), visitor);
        assertThrows(ResourceNotFoundException.class,
                () -> admin.commentVisibility(comment.getId(), "VISIBLE", null, moderator));

        var lastId = notices.list(owner, 1, 20).records().getFirst().id();
        community.comment(post.getId(), visitor, "后来的一条评论");
        notices.readThrough(owner, lastId);
        assertEquals(1, notices.unread(owner).notifications());
        community.comment(post.getId(), visitor, "🐾".repeat(301));
        assertEquals("🐾".repeat(300), notices.list(owner, 1, 20).records().getFirst().content());
    }

    private Long user(String role) {
        SysUser user = new SysUser();
        user.setUsername("flow_" + UUID.randomUUID());
        user.setPasswordHash("unused-test-hash");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        users.insert(user);
        return user.getId();
    }
}
