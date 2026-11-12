package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.ForbiddenException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.entity.DirectMessage;
import com.pawfind.backend.entity.SysUser;
import com.pawfind.backend.mapper.DirectMessageMapper;
import com.pawfind.backend.mapper.SysUserMapper;
import com.pawfind.backend.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DirectMessageService {
    private final DirectMessageMapper messages;
    private final SysUserMapper users;

    public DirectMessageService(DirectMessageMapper messages, SysUserMapper users) {
        this.messages = messages;
        this.users = users;
    }

    public Peer findPeer(String username, Long userId) {
        SysUser peer = users.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username.strip())
                .eq(SysUser::getStatus, "ACTIVE").ne(SysUser::getId, userId));
        if (peer == null) {
            throw new ResourceNotFoundException("未找到可联系的用户");
        }
        return new Peer(peer.getId(), peer.getUsername(), peer.getStatus());
    }

    public Peer peer(Long id, Long userId) {
        if (id.equals(userId)) {
            throw new IllegalArgumentException("请选择其他用户发起私信");
        }
        SysUser peer = users.selectById(id);
        if (peer == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return new Peer(peer.getId(), peer.getUsername(), peer.getStatus());
    }

    public PageVO<DirectMessageMapper.Conversation> conversations(Long userId, int page, int size) {
        long offset = PageVO.offset(page, size);
        long total = messages.countConversations(userId);
        return new PageVO<>(page, size, total, (total + size - 1) / size,
                messages.conversations(userId, offset, size));
    }

    public MessageSlice thread(Long userId, Long partnerId, Long beforeId, Long afterId, int size) {
        peer(partnerId, userId);
        if (size < 1 || size > 50 || (beforeId != null && beforeId < 1)
                || (afterId != null && afterId < 1) || (beforeId != null && afterId != null)) {
            throw new IllegalArgumentException("消息分页参数不合法");
        }
        LambdaQueryWrapper<DirectMessage> query = between(userId, partnerId)
                .lt(beforeId != null, DirectMessage::getId, beforeId)
                .gt(afterId != null, DirectMessage::getId, afterId);
        boolean forward = afterId != null;
        query.orderBy(true, forward, DirectMessage::getId).last("LIMIT " + (size + 1));
        List<DirectMessage> records = new ArrayList<>(messages.selectList(query));
        boolean hasMore = records.size() > size;
        if (hasMore) {
            records.removeLast();
        }
        if (!forward) {
            Collections.reverse(records);
        }
        return new MessageSlice(records, hasMore);
    }

    @Transactional
    public DirectMessage send(Long userId, Long recipientId, String content) {
        peer(recipientId, userId);
        SysUser recipient = users.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getId, recipientId).last("FOR UPDATE"));
        if (!"ACTIVE".equals(recipient.getStatus())) {
            throw new ForbiddenException("对方账号已停用，暂时无法接收私信");
        }
        DirectMessage message = new DirectMessage();
        message.setSenderId(userId);
        message.setRecipientId(recipientId);
        message.setContent(content.strip());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        messages.insert(message);
        return message;
    }

    public void read(Long userId, Long partnerId, Long upToId) {
        messages.update(Wrappers.<DirectMessage>lambdaUpdate()
                .eq(DirectMessage::getSenderId, partnerId).eq(DirectMessage::getRecipientId, userId)
                .le(DirectMessage::getId, upToId).eq(DirectMessage::getIsRead, false)
                .set(DirectMessage::getIsRead, true));
    }

    private LambdaQueryWrapper<DirectMessage> between(Long userId, Long partnerId) {
        return Wrappers.<DirectMessage>lambdaQuery().and(pair -> pair
                .and(outgoing -> outgoing.eq(DirectMessage::getSenderId, userId)
                        .eq(DirectMessage::getRecipientId, partnerId))
                .or(incoming -> incoming.eq(DirectMessage::getSenderId, partnerId)
                        .eq(DirectMessage::getRecipientId, userId)));
    }

    public record Peer(Long id, String username, String status) {
    }

    public record MessageSlice(List<DirectMessage> records, boolean hasMore) {
    }
}
