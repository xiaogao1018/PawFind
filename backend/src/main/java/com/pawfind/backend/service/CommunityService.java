package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.CommunityPostDTO;
import com.pawfind.backend.entity.CommunityComment;
import com.pawfind.backend.entity.CommunityPost;
import com.pawfind.backend.mapper.CommunityCommentMapper;
import com.pawfind.backend.mapper.CommunityPostMapper;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.entity.SysUser;
import java.util.Map;
import java.util.stream.Collectors;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Service
public class CommunityService {
    private final CommunityPostMapper posts;
    private final CommunityCommentMapper comments;
    private final SysUserMapper users;
    private final NotificationService notices;

    public CommunityService(CommunityPostMapper posts, CommunityCommentMapper comments, SysUserMapper users, NotificationService notices) {
        this.posts = posts;
        this.comments = comments;
        this.users = users;
        this.notices = notices;
    }

    public PageVO<CommunityPost> listPublic(int page, int size) {
        return listPublic(page, size, null);
    }

    public PageVO<CommunityPost> listPublic(int page, int size, String category) {
        PageVO<CommunityPost> result = PageVO.query(posts, Wrappers.<CommunityPost>lambdaQuery()
                        .eq(CommunityPost::getAuditStatus, "APPROVED")
                        .eq(CommunityPost::getVisibilityStatus, "VISIBLE")
                        .eq(category != null, CommunityPost::getCategory, category),
                CommunityPost::getId, page, size, Function.identity());
        decorate(result.records());
        return result;
    }

    public PageVO<CommunityPost> listMine(Long userId, int page, int size) {
        PageVO<CommunityPost> result = PageVO.query(posts, Wrappers.<CommunityPost>lambdaQuery()
                        .eq(CommunityPost::getUserId, userId),
                CommunityPost::getId, page, size, Function.identity());
        decorate(result.records());
        return result;
    }

    public CommunityPost get(long id, Long userId, String role) {
        CommunityPost post = requirePost(posts.selectById(id));
        if (!isPublic(post) && !userId.equals(post.getUserId()) && !"ADMIN".equals(role)) {
            throw new ResourceNotFoundException("帖子不存在或不可见");
        }
        decorate(List.of(post));
        return post;
    }

    @Transactional
    public CommunityPost create(CommunityPostDTO dto, Long userId) {
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(dto.title().strip());
        post.setContent(dto.content().strip());
        post.setCategory(dto.category() == null ? "HELP" : dto.category());
        post.setAuditStatus("PENDING");
        post.setVisibilityStatus("VISIBLE");
        post.setCreatedAt(LocalDateTime.now());
        posts.insert(post);
        return post;
    }

    @Transactional
    public CommunityPost update(long id, CommunityPostDTO dto, Long userId) {
        CommunityPost post = lockPost(id);
        if (!userId.equals(post.getUserId())) {
            throw new ForbiddenException("只能编辑自己的帖子");
        }

        posts.update(Wrappers.<CommunityPost>lambdaUpdate().eq(CommunityPost::getId, id)
                .set(CommunityPost::getTitle, dto.title().strip())
                .set(CommunityPost::getContent, dto.content().strip())
                .set(dto.category() != null, CommunityPost::getCategory, dto.category())
                .set(CommunityPost::getAuditStatus, "PENDING")
                .set(CommunityPost::getVisibilityStatus, "VISIBLE")
                .set(CommunityPost::getModerationReason, null));
        return posts.selectById(id);
    }

    @Transactional(readOnly = true)
    public List<CommunityComment> listComments(long postId, Long userId, String role) {
        if (!isPublic(get(postId, userId, role))) {
            return List.of();
        }
        return comments.selectList(Wrappers.<CommunityComment>lambdaQuery()
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getVisibilityStatus, "VISIBLE")
                .orderByAsc(CommunityComment::getId));
    }

    @Transactional
    public CommunityComment comment(long postId, Long userId, String content) {
        CommunityPost post = lockPost(postId);
        if (!isPublic(post)) {
            throw new ResourceNotFoundException("帖子不存在或不可评论");
        }

        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.strip());
        comment.setVisibilityStatus("VISIBLE");
        comment.setCreatedAt(LocalDateTime.now());
        comments.insert(comment);
        notices.notify(post.getUserId(), userId, "COMMENT", "POST", postId, content.strip());
        return comment;
    }

    @Transactional
    public void deleteComment(long postId, long commentId, Long userId) {
        CommunityComment comment = comments.selectOne(Wrappers.<CommunityComment>lambdaQuery()
                .eq(CommunityComment::getId, commentId)
                .eq(CommunityComment::getPostId, postId).last("FOR UPDATE"));
        if (comment == null) {
            throw new ResourceNotFoundException("评论不存在");
        }
        if (!userId.equals(comment.getUserId())) {
            throw new ForbiddenException("只能删除自己的评论");
        }
        comments.update(Wrappers.<CommunityComment>lambdaUpdate().eq(CommunityComment::getId, commentId)
                .set(CommunityComment::getVisibilityStatus, "DELETED"));
    }

    CommunityPost lockPost(long id) {
        return requirePost(posts.selectOne(Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getId, id).last("FOR UPDATE")));
    }

    private void decorate(List<CommunityPost> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> authorIds = records.stream().map(CommunityPost::getUserId).distinct().toList();
        Map<Long, String> names = users.selectByIds(authorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
        Map<Long, Long> totals = comments.countVisibleByPosts(
                        records.stream().map(CommunityPost::getId).toList()).stream()
                .collect(Collectors.toMap(CommunityCommentMapper.CommentTotal::postId,
                        CommunityCommentMapper.CommentTotal::total));
        for (CommunityPost post : records) {
            post.setAuthorName(names.getOrDefault(post.getUserId(), "宠友"));
            post.setCommentCount(totals.getOrDefault(post.getId(), 0L));
        }
    }

    private CommunityPost requirePost(CommunityPost post) {
        if (post == null) {
            throw new ResourceNotFoundException("帖子不存在");
        }
        return post;
    }

    private boolean isPublic(CommunityPost post) {
        return "APPROVED".equals(post.getAuditStatus()) && "VISIBLE".equals(post.getVisibilityStatus());
    }
}
