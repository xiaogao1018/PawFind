package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.entity.*;
import com.pawfind.backend.mapper.*;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecordCommentService {
    private final RecordCommentMapper comments;
    private final PetLostMapper lost;
    private final PetFoundMapper found;
    private final SysUserMapper users;
    private final NotificationService notices;

    public RecordCommentService(RecordCommentMapper comments, PetLostMapper lost, PetFoundMapper found,
                                SysUserMapper users, NotificationService notices) {
        this.comments = comments;
        this.lost = lost;
        this.found = found;
        this.users = users;
        this.notices = notices;
    }

    public PageVO<Comment> list(boolean isLost, Long recordId, int page, int size) {
        requireOwner(isLost, recordId, false);
        var query = Wrappers.<RecordComment>lambdaQuery()
                .eq(isLost, RecordComment::getPetLostId, recordId)
                .eq(!isLost, RecordComment::getPetFoundId, recordId);
        var data = PageVO.query(comments, query, RecordComment::getId, page, size, Function.identity());
        Map<Long, String> names = Map.of();
        if (!data.records().isEmpty()) {
            names = users.selectByIds(data.records().stream().map(RecordComment::getUserId).distinct().toList())
                    .stream().collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
        }
        Map<Long, String> authors = names;
        return new PageVO<>(page, size, data.total(), data.totalPages(), data.records().stream()
                .map(item -> new Comment(item.getId(), item.getUserId(),
                        authors.getOrDefault(item.getUserId(), "宠友"), item.getContent(), item.getCreatedAt()))
                .toList());
    }

    @Transactional
    public Comment create(boolean isLost, Long recordId, Long userId, String content) {
        Long ownerId = requireOwner(isLost, recordId, true);
        RecordComment comment = new RecordComment();
        if (isLost) {
            comment.setPetLostId(recordId);
        } else {
            comment.setPetFoundId(recordId);
        }
        comment.setUserId(userId);
        comment.setContent(content.strip());
        comment.setCreatedAt(LocalDateTime.now());
        comments.insert(comment);
        notices.notify(ownerId, userId, "RECORD_COMMENT", isLost ? "LOST" : "FOUND", recordId, content.strip());
        return new Comment(comment.getId(), userId, users.selectById(userId).getUsername(),
                comment.getContent(), comment.getCreatedAt());
    }

    private Long requireOwner(boolean isLost, Long recordId, boolean lock) {
        if (isLost) {
            var query = Wrappers.<PetLost>lambdaQuery().eq(PetLost::getId, recordId);
            if (lock) {
                query.last("FOR UPDATE");
            }
            PetLost record = lost.selectOne(query);
            if (record == null || !"VISIBLE".equals(record.getVisibilityStatus())
                    || !"LOOKING".equals(record.getStatus()) && !"FOUND".equals(record.getStatus())) {
                throw new ResourceNotFoundException("走失信息不存在或不可见");
            }
            return record.getUserId();
        }
        var query = Wrappers.<PetFound>lambdaQuery().eq(PetFound::getId, recordId);
        if (lock) {
            query.last("FOR UPDATE");
        }
        PetFound record = found.selectOne(query);
        if (record == null || !"VISIBLE".equals(record.getVisibilityStatus()) || !"OPEN".equals(record.getStatus())) {
            throw new ResourceNotFoundException("招领信息不存在或不可见");
        }
        return record.getUserId();
    }

    public record Comment(Long id, Long userId, String username, String content, LocalDateTime createdAt) {
    }
}
