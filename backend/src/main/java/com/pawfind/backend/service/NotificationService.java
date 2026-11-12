package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.entity.UserNotification;
import com.pawfind.backend.mapper.UserNotificationMapper;
import com.pawfind.backend.mapper.DirectMessageMapper;
import com.pawfind.backend.entity.DirectMessage;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final UserNotificationMapper notifications;
    private final DirectMessageMapper messages;
    private final SysUserMapper users;

    public NotificationService(UserNotificationMapper notifications, DirectMessageMapper messages,
                               SysUserMapper users) {
        this.notifications = notifications;
        this.messages = messages;
        this.users = users;
    }

    // The caller's business transaction also owns this notification insert.
    public void notify(Long recipientId, Long actorId, String type, String targetType, Long targetId,
                       String content) {
        if (recipientId.equals(actorId)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        int previewLength = Math.min(300, content.codePointCount(0, content.length()));
        notification.setContent(content.substring(0, content.offsetByCodePoints(0, previewLength)));
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notifications.insert(notification);
    }

    public PageVO<Notice> list(Long userId, int page, int size) {
        PageVO<UserNotification> data = PageVO.query(notifications,
                Wrappers.<UserNotification>lambdaQuery().eq(UserNotification::getRecipientId, userId),
                UserNotification::getId, page, size, java.util.function.Function.identity());
        Map<Long, String> names = Map.of();
        if (!data.records().isEmpty()) {
            names = users.selectByIds(data.records().stream().map(UserNotification::getActorId).distinct().toList())
                    .stream().collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
        }
        Map<Long, String> actorNames = names;
        return new PageVO<>(page, size, data.total(), data.totalPages(), data.records().stream()
                .map(notice -> new Notice(notice.getId(), notice.getActorId(),
                        actorNames.getOrDefault(notice.getActorId(), "宠友"), notice.getType(),
                        notice.getTargetType(), notice.getTargetId(), notice.getContent(),
                        notice.getIsRead(), notice.getCreatedAt())).toList());
    }

    public Unread unread(Long userId) {
        return new Unread(notifications.selectCount(Wrappers.<UserNotification>lambdaQuery()
                        .eq(UserNotification::getRecipientId, userId).eq(UserNotification::getIsRead, false)),
                messages.selectCount(Wrappers.<DirectMessage>lambdaQuery()
                        .eq(DirectMessage::getRecipientId, userId).eq(DirectMessage::getIsRead, false)));
    }

    public void read(Long userId, Long id) {
        notifications.update(Wrappers.<UserNotification>lambdaUpdate()
                .eq(UserNotification::getRecipientId, userId).eq(UserNotification::getId, id)
                .set(UserNotification::getIsRead, true));
    }

    public void readThrough(Long userId, Long upToId) {
        notifications.update(Wrappers.<UserNotification>lambdaUpdate()
                .eq(UserNotification::getRecipientId, userId).le(UserNotification::getId, upToId)
                .eq(UserNotification::getIsRead, false).set(UserNotification::getIsRead, true));
    }

    public record Unread(long notifications, long direct) {
    }

    public record Notice(Long id, Long actorId, String actorName, String type, String targetType,
                         Long targetId, String content, boolean isRead, LocalDateTime createdAt) {
    }
}
