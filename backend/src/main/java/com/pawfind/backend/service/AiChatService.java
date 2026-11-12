package com.pawfind.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawfind.backend.common.exception.AiServiceException;
import com.pawfind.backend.common.exception.ResourceNotFoundException;
import com.pawfind.backend.dto.AiChatRequest;
import com.pawfind.backend.entity.AiChatMessage;
import com.pawfind.backend.entity.AiChatSession;
import com.pawfind.backend.mapper.AiChatMessageMapper;
import com.pawfind.backend.mapper.AiChatSessionMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiChatService {
    private static final String BASE_SYSTEM_PROMPT = """
            你是 PawFind 的 AI 寻宠助手。请始终使用简洁、温和、可操作的中文回答。
            不要编造用户没有提供的宠物特征、地点、时间、联系方式或寻回结果；信息不足时先提出具体问题。
            不要把推测写成事实，不保证一定能寻回宠物。提醒用户保护住址、证件号等敏感信息。
            涉及宠物健康时只提供一般知识，不做确诊或替代兽医；出现呼吸困难、大量出血、中毒、抽搐等紧急情况时建议立即联系正规宠物医院。
            """;

    private final AiChatSessionMapper sessions;
    private final AiChatMessageMapper messages;
    private final ChatClient chatClient;
    private final boolean configured;
    private final String model;
    private final int contextMessageLimit;
    private final Duration streamTimeout;

    public AiChatService(AiChatSessionMapper sessions,
                         AiChatMessageMapper messages,
                         ObjectProvider<ChatClient.Builder> chatClientBuilders,
                         @Value("${spring.ai.model.chat:none}") String provider,
                         @Value("${spring.ai.deepseek.api-key:}") String apiKey,
                         @Value("${spring.ai.deepseek.chat.model:deepseek-flash}") String model,
                         @Value("${pawfind.ai.context-message-limit:20}") int contextMessageLimit,
                         @Value("${pawfind.ai.stream-timeout-seconds:90}") long streamTimeoutSeconds) {
        this.sessions = sessions;
        this.messages = messages;
        this.model = model;
        this.contextMessageLimit = Math.max(2, Math.min(contextMessageLimit, 50));
        this.streamTimeout = Duration.ofSeconds(Math.max(10, Math.min(streamTimeoutSeconds, 300)));
        boolean deepSeekRequested = "deepseek".equalsIgnoreCase(provider) && !apiKey.isBlank();
        ChatClient.Builder builder = deepSeekRequested ? chatClientBuilders.getIfAvailable() : null;
        this.configured = deepSeekRequested && builder != null;
        this.chatClient = configured ? builder.build() : null;
    }

    public Status status() {
        String message = configured
                ? "DeepSeek 已连接，可以开始对话"
                : "AI 尚未启用，请配置 DEEPSEEK_API_KEY 和 SPRING_AI_MODEL_CHAT";
        return new Status(configured, configured ? "DeepSeek" : null, configured ? model : null, message);
    }

    public List<SessionSummary> listSessions(Long userId) {
        return sessions.selectList(Wrappers.<AiChatSession>lambdaQuery()
                        .eq(AiChatSession::getUserId, userId)
                        .orderByDesc(AiChatSession::getUpdatedAt)
                        .last("LIMIT 50"))
                .stream().map(this::summary).toList();
    }

    public SessionSummary createSession(Long userId, String requestedTitle) {
        String title = requestedTitle == null || requestedTitle.isBlank()
                ? "新对话" : truncate(requestedTitle.strip(), 100);
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(session.getCreatedAt());
        sessions.insert(session);
        return summary(session);
    }

    public List<AiChatMessage> listMessages(Long userId, Long sessionId) {
        ownedSession(userId, sessionId);
        return messages.selectList(Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getId));
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        ownedSession(userId, sessionId);
        messages.delete(Wrappers.<AiChatMessage>lambdaQuery().eq(AiChatMessage::getSessionId, sessionId));
        sessions.delete(Wrappers.<AiChatSession>lambdaQuery()
                .eq(AiChatSession::getId, sessionId).eq(AiChatSession::getUserId, userId));
    }

    public ChatReply chat(Long userId, AiChatRequest request) {
        requireConfigured();
        PreparedChat prepared = prepare(userId, request);
        try {
            String answer = chatClient.prompt().messages(prepared.promptMessages()).call().content();
            if (answer == null || answer.isBlank()) {
                throw new AiServiceException("DeepSeek 没有返回有效内容，请重新提问");
            }
            AiChatMessage assistant = saveMessage(prepared.sessionId(), "ASSISTANT", answer.strip());
            touch(prepared.sessionId());
            return new ChatReply(prepared.sessionId(), prepared.userMessage(), assistant);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw friendlyFailure(e);
        }
    }

    public Flux<StreamEvent> stream(Long userId, AiChatRequest request) {
        requireConfigured();
        return Flux.defer(() -> {
            PreparedChat prepared = prepare(userId, request);
            StringBuilder answer = new StringBuilder();
            AtomicBoolean persisted = new AtomicBoolean(false);

            Runnable persistAnswer = () -> {
                if (persisted.compareAndSet(false, true) && !answer.isEmpty()) {
                    saveMessage(prepared.sessionId(), "ASSISTANT", answer.toString());
                    touch(prepared.sessionId());
                }
            };

            Flux<StreamEvent> modelEvents;
            try {
                modelEvents = chatClient.prompt().messages(prepared.promptMessages()).stream().content()
                        .filter(delta -> delta != null && !delta.isEmpty())
                        .map(delta -> {
                            answer.append(delta);
                            return StreamEvent.delta(delta);
                        })
                        .timeout(streamTimeout)
                        .concatWith(Flux.defer(() -> {
                            persistAnswer.run();
                            return Flux.just(StreamEvent.done());
                        }))
                        .onErrorResume(error -> {
                            persistAnswer.run();
                            return Flux.just(StreamEvent.error(friendlyFailure(error).getMessage()));
                        })
                        .doOnCancel(persistAnswer);
            } catch (Exception e) {
                modelEvents = Flux.just(StreamEvent.error(friendlyFailure(e).getMessage()));
            }
            return Flux.concat(Flux.just(StreamEvent.meta(prepared.sessionId())), modelEvents);
        });
    }

    private PreparedChat prepare(Long userId, AiChatRequest request) {
        String content = request.content().strip();
        Long sessionId;
        AiChatSession session;
        if (request.sessionId() == null) {
            SessionSummary created = createSession(userId, titleFrom(content));
            sessionId = created.id();
            session = ownedSession(userId, sessionId);
        } else {
            sessionId = request.sessionId();
            session = ownedSession(userId, sessionId);
        }

        AiChatMessage userMessage = saveMessage(sessionId, "USER", content);
        if ("新对话".equals(session.getTitle())) {
            session.setTitle(titleFrom(content));
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessions.updateById(session);

        List<AiChatMessage> context = messages.selectList(Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT " + contextMessageLimit));
        Collections.reverse(context);

        List<Message> prompt = new ArrayList<>();
        prompt.add(new SystemMessage(systemPrompt(request.resolvedType())));
        for (AiChatMessage item : context) {
            if ("USER".equals(item.getRole())) {
                prompt.add(new UserMessage(item.getContent()));
            } else if ("ASSISTANT".equals(item.getRole())) {
                prompt.add(AssistantMessage.builder().content(item.getContent()).build());
            }
        }
        return new PreparedChat(sessionId, userMessage, prompt);
    }

    private String systemPrompt(AiAssistantType type) {
        String task = switch (type) {
            case LOST_INFO -> "当前任务是整理寻宠资料。按名称、种类、品种、性别、毛色体型、显著特征、走失时间地点、联系方式和注意事项检查信息；缺失项用问题引导补充，不要自行填造。";
            case NOTICE_DRAFT -> "当前任务是生成可编辑的寻宠启事草稿。先核对必要事实，再用标题、基本信息、走失经过、识别特征、联系方式、温馨提示组织正文；未知内容保留清晰的待填写标记。";
            case PET_QA -> "当前任务是养宠问答。区分一般护理建议与医疗判断，给出风险等级和下一步行动；信息不足时询问物种、年龄、症状、持续时间和既往情况。";
            case GENERAL -> "当前任务是一般寻宠与养宠咨询。回答应围绕用户问题，必要时建议切换到资料整理或启事草稿。";
        };
        return BASE_SYSTEM_PROMPT + "\n" + task;
    }

    private AiChatSession ownedSession(Long userId, Long sessionId) {
        if (sessionId == null || sessionId < 1) {
            throw new IllegalArgumentException("会话编号不合法");
        }
        AiChatSession session = sessions.selectOne(Wrappers.<AiChatSession>lambdaQuery()
                .eq(AiChatSession::getId, sessionId).eq(AiChatSession::getUserId, userId));
        if (session == null) {
            throw new ResourceNotFoundException("AI 会话不存在");
        }
        return session;
    }

    private AiChatMessage saveMessage(Long sessionId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messages.insert(message);
        return message;
    }

    private void touch(Long sessionId) {
        sessions.update(Wrappers.<AiChatSession>lambdaUpdate()
                .eq(AiChatSession::getId, sessionId)
                .set(AiChatSession::getUpdatedAt, LocalDateTime.now()));
    }

    private void requireConfigured() {
        if (!configured) {
            throw new AiServiceException("AI 尚未启用，请先配置 DeepSeek API Key");
        }
    }

    private AiServiceException friendlyFailure(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String details = (String.valueOf(error.getMessage()) + " " + String.valueOf(current.getMessage())).toLowerCase(Locale.ROOT);
        if (error instanceof TimeoutException || details.contains("timeout") || details.contains("timed out")) {
            return new AiServiceException("DeepSeek 响应超时，请稍后重试", error);
        }
        if (details.contains("401") || details.contains("unauthorized") || details.contains("authentication")) {
            return new AiServiceException("DeepSeek API Key 无效，请检查环境变量", error);
        }
        if (details.contains("402") || details.contains("insufficient balance")) {
            return new AiServiceException("DeepSeek 账户余额不足，请充值后重试", error);
        }
        if (details.contains("429") || details.contains("rate limit")) {
            return new AiServiceException("AI 请求过于频繁，请稍后重试", error);
        }
        return new AiServiceException("DeepSeek 暂时无法回复，请稍后重试", error);
    }

    private SessionSummary summary(AiChatSession session) {
        return new SessionSummary(session.getId(), session.getTitle(), session.getCreatedAt(), session.getUpdatedAt());
    }

    private String titleFrom(String content) {
        return truncate(content.replaceAll("\\s+", " "), 24);
    }

    private String truncate(String value, int maxCodePoints) {
        int count = value.codePointCount(0, value.length());
        if (count <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints)) + "…";
    }

    private record PreparedChat(Long sessionId, AiChatMessage userMessage, List<Message> promptMessages) {
    }

    public record Status(boolean configured, String provider, String model, String message) {
    }

    public record SessionSummary(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record ChatReply(Long sessionId, AiChatMessage userMessage, AiChatMessage assistantMessage) {
    }

    public record StreamEvent(String event, Long sessionId, String content, String message) {
        public static StreamEvent meta(Long sessionId) {
            return new StreamEvent("meta", sessionId, null, null);
        }

        public static StreamEvent delta(String content) {
            return new StreamEvent("delta", null, content, null);
        }

        public static StreamEvent done() {
            return new StreamEvent("done", null, null, null);
        }

        public static StreamEvent error(String message) {
            return new StreamEvent("error", null, null, message);
        }
    }
}
