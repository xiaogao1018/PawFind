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

## DeepSeek AI

项目通过 Spring AI 2.0.x 的 DeepSeek Starter 调用模型。服务端只从环境变量读取 API Key。未设置 `DEEPSEEK_API_KEY` 和 `SPRING_AI_MODEL_CHAT=deepseek` 时，GET `/api/ai/status` 返回 `configured=false`，流式接口返回 503 JSON，其他业务不受影响。

### 会话与历史

- GET `/api/ai/status` 返回启用状态、供应商、模型名和提示。
- GET `/api/ai/sessions` 返回当前用户最近更新的 50 个会话。
- POST `/api/ai/sessions` 接收可选的 `{"title":"新对话"}`。
- GET `/api/ai/sessions/{sessionId}/messages` 返回自己的会话消息。
- DELETE `/api/ai/sessions/{sessionId}` 删除自己的会话及消息。

所有会话查询都同时匹配 `sessionId` 和当前登录用户 ID。模型请求携带最近 20 条消息作为多轮上下文，避免历史无限增长导致 token 成本和延迟持续上升。

### 非流式问答

POST `/api/ai/chat` 接收以下请求，并返回已保存的用户消息和 AI 消息。

```json
{
  "sessionId": 12,
  "content": "帮我整理寻宠信息",
  "assistantType": "LOST_INFO"
}
```

`sessionId` 可以省略，服务端会自动创建会话。`assistantType` 可取 `GENERAL`、`LOST_INFO`、`NOTICE_DRAFT` 或 `PET_QA`，省略时使用 `GENERAL`。

### SSE 流式问答

POST `/api/ai/chat/stream` 使用相同请求体。成功响应为 `Content-Type: text/event-stream; charset=utf-8`。第一个 `meta` 事件返回会话 ID，随后发送增量文本。


```text
event: meta
data: {"event":"meta","sessionId":12}

event: delta
data: {"event":"delta","content":"请先告诉我"}

event: delta
data: {"event":"delta","content":"宠物的种类。"}

event: done
data: {"event":"done"}

```

流建立后的错误使用 `error` 事件并结束响应。已经生成的部分内容会保存到历史记录。

```text
event: error
data: {"event":"error","message":"DeepSeek 暂时无法回复，请稍后重试"}

```

在流开始前发生的错误使用对应 HTTP 状态及 Result JSON。前端使用 `fetch` 和 `ReadableStream` 解析 SSE，支持 UTF-8 分块、停止生成、异常提示和切换历史会话。

### 安全与提示词

系统提示词要求模型不编造宠物特征、地点、联系方式或寻回结果；资料不足时先追问。养宠问答只提供一般知识，紧急症状会建议联系正规宠物医院。用户仍应核实模型输出，不能把 AI 回复当作医疗诊断或确定事实。
