
# PawFind 用户模块 API 文档

## 1. 根据 ID 查询用户

### 基本信息

- 请求方法：GET
- 请求路径：/api/users/{id}
- 请求参数：id（Long，路径参数，必须大于 0）
- 响应格式：JSON

### 成功示例

请求：

GET /api/users/1

响应状态：HTTP 200

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "test_user",
    "role": "USER"
  }
}
```

### 错误情况

| 场景 | HTTP 状态码 | message |
|---|---|---|
| 用户 ID 不合法（如 0） | 400 | 用户ID必须大于0 |
| 用户不存在 | 404 | 用户不存在 |

### 安全说明

当前接口仅用于本地开发测试，尚未实现登录鉴权。
正式开放前必须添加访问权限校验，避免未经授权查询用户信息。
响应中不得包含 passwordHash。
