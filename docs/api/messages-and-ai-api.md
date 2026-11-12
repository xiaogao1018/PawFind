# 消息与 AI 接入

所有消息和 AI 接口需要 Bearer Token。普通 JSON 响应使用项目现有的 Result 格式。

## 互动通知

- GET `/api/messages/notifications?page=1&size=20` 查询当前用户的通知。
- GET `/api/messages/unread` 返回 `notifications` 和 `direct` 两项未读数量。
- PATCH `/api/messages/notifications/{id}/read` 将自己的通知标为已读。
- PATCH `/api/messages/notifications/read` 接收 `{"upToId":123}`，读取截至该 ID 的通知。

走失和招领留言、目击线索、社区评论、帖子审核及信息隐藏或恢复会产生通知。通知与对应业务修改处于同一数据库事务。

## 私信

- GET `/api/messages/recipients?username=xxx` 按完整用户名查找可联系账号。
- GET `/api/messages/conversations?page=1&size=20` 查询当前用户参与的会话。
- GET `/api/messages/conversations/{userId}/messages` 返回与对方的最近 30 条消息，按 ID 升序排列。
- 可用 `beforeId` 向前读取历史消息，或用 `afterId` 同步新消息。两者互斥。`hasMore` 表示该方向还有消息。
- POST `/api/messages/conversations/{userId}/messages` 接收 `{"content":"你好"}`。
- PATCH `/api/messages/conversations/{userId}/read` 接收 `{"upToId":123}`。

读取或标记已读始终以当前登录用户和对方用户组成的会话为范围。标记已读使用已获取的最大消息 ID，保留此后到达消息的未读状态。

## 流式 AI

当前 GET `/api/ai/status` 返回 `configured=false`。POST `/api/ai/chat/stream` 返回 HTTP 503 的 Result 错误，界面展示尚未开放的提示。

接入模型时，替换 AiChatController 中的占位响应，并将 status 的 configured 改为实际服务状态。前端已使用 fetch 流式读取，支持停止和连接中断提示。

请求格式为

```json
{"messages":[{"role":"user","content":"帮我整理寻宠信息"}]}
```

成功响应使用 `Content-Type: text/event-stream; charset=utf-8`。每个增量通过 delta 事件发送，data 是 JSON，包含本次新增文本。

```text
event: delta
data: {"content":"请先告诉我"}

event: delta
data: {"content":"宠物的种类。"}

event: done
data: {}

```

流开始后的错误使用 error 事件，随后结束响应。

```text
event: error
data: {"message":"服务暂时繁忙，请稍后重试"}

```

在流开始前发生的错误使用对应 HTTP 状态及 Result JSON。模型调用和凭据配置放在服务端，并保留现有的登录鉴权。前端适配位于 `frontend/src/api/ai.ts`，对话界面位于 `frontend/src/components/AiChatPanel.vue`。
