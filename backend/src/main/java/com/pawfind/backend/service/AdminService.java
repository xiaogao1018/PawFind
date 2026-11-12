package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.entity.AdminOperationLog;
import com.pawfind.backend.entity.CommunityPost;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.entity.PetLost;
import com.pawfind.backend.entity.PetFound;
import com.pawfind.backend.entity.CommunityComment;
import com.pawfind.backend.mapper.PetLostMapper;
import com.pawfind.backend.mapper.PetFoundMapper;
import com.pawfind.backend.mapper.CommunityCommentMapper;
import com.pawfind.backend.mapper.AdminOperationLogMapper;
import com.pawfind.backend.mapper.CommunityPostMapper;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
public class AdminService {
    private final CommunityService community;
    private final CommunityPostMapper posts;
    private final SysUserMapper users;
    private final AdminOperationLogMapper logs;
    private final PetLostMapper lost;
    private final PetFoundMapper found;
    private final CommunityCommentMapper comments;
    private final NotificationService notices;

    public AdminService(CommunityService community, CommunityPostMapper posts,
                        SysUserMapper users, AdminOperationLogMapper logs, PetLostMapper lost,
                        PetFoundMapper found, CommunityCommentMapper comments, NotificationService notices) {
        this.community = community;
        this.posts = posts;
        this.users = users;
        this.logs = logs;
        this.lost = lost;
        this.found = found;
        this.comments = comments;
        this.notices = notices;
    }

    public PageVO<CommunityPost> listPosts(String auditStatus, String visibilityStatus, int page, int size) {
        return PageVO.query(posts, Wrappers.<CommunityPost>lambdaQuery()
                        .eq(auditStatus != null, CommunityPost::getAuditStatus, auditStatus)
                        .eq(visibilityStatus != null, CommunityPost::getVisibilityStatus, visibilityStatus),
                CommunityPost::getId, page, size, Function.identity());
    }

    @Transactional
    public CommunityPost audit(long id, String status, String reason, Long adminId) {
        CommunityPost post = community.lockPost(id);
        if (!"PENDING".equals(post.getAuditStatus())) {
            throw new IllegalArgumentException("只有待审核帖子可以审核");
        }
        String detail = cleanReason(reason, "REJECTED".equals(status));
        posts.update(Wrappers.<CommunityPost>lambdaUpdate().eq(CommunityPost::getId, id)
                .set(CommunityPost::getAuditStatus, status)
                .set(CommunityPost::getVisibilityStatus, "VISIBLE")
                .set(CommunityPost::getModerationReason, "REJECTED".equals(status) ? detail : null));
        log(adminId, "COMMUNITY_POST", id, "APPROVED".equals(status) ? "APPROVE" : "REJECT", detail);
        notices.notify(post.getUserId(), adminId, "AUDIT", "POST", id,
                "APPROVED".equals(status) ? "你的帖子已通过审核" : "帖子审核未通过：" + detail);
        return posts.selectById(id);
    }

    @Transactional
    public CommunityPost setVisibility(long id, String status, String reason, Long adminId) {
        CommunityPost post = community.lockPost(id);
        if (!"APPROVED".equals(post.getAuditStatus())) {
            throw new IllegalArgumentException("只有已通过审核的帖子可以隐藏或恢复");
        }
        if (status.equals(post.getVisibilityStatus())) {
            return post;
        }
        boolean hiding = "HIDDEN".equals(status);
        String detail = cleanReason(reason, hiding);
        posts.update(Wrappers.<CommunityPost>lambdaUpdate().eq(CommunityPost::getId, id)
                .set(CommunityPost::getVisibilityStatus, status)
                .set(CommunityPost::getModerationReason, hiding ? detail : null));
        log(adminId, "COMMUNITY_POST", id, hiding ? "HIDE" : "RESTORE", detail);
        return posts.selectById(id);
    }

    public PageVO<AdminUser> listUsers(int page, int size) {
        return PageVO.query(users, Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getRole,
                                SysUser::getStatus, SysUser::getCreatedAt),
                SysUser::getId, page, size, AdminUser::from);
    }

    @Transactional
    public AdminUser updateUserStatus(Long id, String status, Long adminId) {
        if (adminId.equals(id) && "DISABLED".equals(status)) {
            throw new ForbiddenException("不能禁用当前管理员账号");
        }

        SysUser user = users.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getId, id).last("FOR UPDATE"));
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        if (!status.equals(user.getStatus())) {
            users.update(Wrappers.<SysUser>lambdaUpdate().eq(SysUser::getId, id)
                    .set(SysUser::getStatus, status));
            log(adminId, "USER", id, "DISABLED".equals(status) ? "DISABLE" : "ENABLE", null);
            user.setStatus(status);
        }
        return AdminUser.from(user);
    }

    public PageVO<AdminOperationLog> listLogs(int page, int size) {
        return PageVO.query(logs, Wrappers.lambdaQuery(), AdminOperationLog::getId, page, size,
                Function.identity());
    }

    public PageVO<PetLost> listLost(String visibility, int page, int size) {
        return PageVO.query(lost, Wrappers.<PetLost>lambdaQuery()
                        .eq(visibility != null, PetLost::getVisibilityStatus, visibility),
                PetLost::getId, page, size, Function.identity());
    }

    public PageVO<PetFound> listFound(String visibility, int page, int size) {
        return PageVO.query(found, Wrappers.<PetFound>lambdaQuery()
                        .eq(visibility != null, PetFound::getVisibilityStatus, visibility),
                PetFound::getId, page, size, Function.identity());
    }

    @Transactional
    public void recordVisibility(boolean isLost, Long id, String status, String reason, Long adminId) {
        String detail = cleanReason(reason, "HIDDEN".equals(status));
        Long ownerId;
        if (isLost) {
            PetLost record = lost.selectOne(Wrappers.<PetLost>lambdaQuery()
                    .eq(PetLost::getId, id).last("FOR UPDATE"));
            if (record == null) {
                throw new ResourceNotFoundException("走失记录不存在");
            }
            if (status.equals(record.getVisibilityStatus())) {
                return;
            }
            ownerId = record.getUserId();
            lost.update(Wrappers.<PetLost>lambdaUpdate().eq(PetLost::getId, id)
                    .set(PetLost::getVisibilityStatus, status)
                    .set(PetLost::getModerationReason, "HIDDEN".equals(status) ? detail : null));
        } else {
            PetFound record = found.selectOne(Wrappers.<PetFound>lambdaQuery()
                    .eq(PetFound::getId, id).last("FOR UPDATE"));
            if (record == null) {
                throw new ResourceNotFoundException("招领记录不存在");
            }
            if (status.equals(record.getVisibilityStatus())) {
                return;
            }
            ownerId = record.getUserId();
            found.update(Wrappers.<PetFound>lambdaUpdate().eq(PetFound::getId, id)
                    .set(PetFound::getVisibilityStatus, status)
                    .set(PetFound::getModerationReason, "HIDDEN".equals(status) ? detail : null));
        }
        boolean hiding = "HIDDEN".equals(status);
        log(adminId, isLost ? "PET_LOST" : "PET_FOUND", id, hiding ? "HIDE" : "RESTORE", detail);
        notices.notify(ownerId, adminId, "MODERATION", isLost ? "LOST" : "FOUND", id,
                hiding ? "你发布的信息已隐藏：" + detail : "你发布的信息已恢复展示");
    }

    public PageVO<CommunityComment> listComments(String visibility, int page, int size) {
        return PageVO.query(comments, Wrappers.<CommunityComment>lambdaQuery()
                        .eq(visibility != null, CommunityComment::getVisibilityStatus, visibility),
                CommunityComment::getId, page, size, Function.identity());
    }

    @Transactional
    public void commentVisibility(Long id, String status, String reason, Long adminId) {
        CommunityComment comment = comments.selectOne(Wrappers.<CommunityComment>lambdaQuery()
                .eq(CommunityComment::getId, id).last("FOR UPDATE"));
        if (comment == null || "DELETED".equals(comment.getVisibilityStatus())) {
            throw new ResourceNotFoundException("评论不存在或已被作者删除");
        }
        if (status.equals(comment.getVisibilityStatus())) {
            return;
        }
        boolean hiding = "HIDDEN".equals(status);
        String detail = cleanReason(reason, hiding);
        comments.update(Wrappers.<CommunityComment>lambdaUpdate().eq(CommunityComment::getId, id)
                .set(CommunityComment::getVisibilityStatus, status));
        log(adminId, "COMMUNITY_COMMENT", id, hiding ? "HIDE" : "RESTORE", detail);
    }

    private String cleanReason(String reason, boolean required) {
        if (reason == null || reason.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("请填写操作原因");
            }
            return null;
        }
        return reason.strip();
    }

    private void log(Long adminId, String targetType, long targetId, String operation, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setOperation(operation);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        logs.insert(log);
    }

    public record AdminUser(Long id, String username, String role, String status, LocalDateTime createdAt) {
        static AdminUser from(SysUser user) {
            return new AdminUser(user.getId(), user.getUsername(), user.getRole(), user.getStatus(),
                    user.getCreatedAt());
        }
    }
}
