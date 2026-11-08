# PawFind 认证模块 API 文档

## 1. 用户注册

**请求方式：** POST

**请求路径：** `/api/auth/register`

**请求体（JSON）：**

```json
{
  "username": "xiaoming",
  "password": "abc123456"
}
```

### 参数要求

| 参数 | 类型 | 要求 |
|---|---|---|
| username | String | 非空，长度 3～50 个字符 |
| password | String | 非空，长度 8～72 个字符，且 UTF-8 编码不超过 72 字节 |

### 注册成功

HTTP 状态码：201 Created

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2,
    "username": "xiaoming",
    "role": "USER"
  }
}
```

注：`id` 由数据库生成，示例仅供参考。当前成功响应体的 `code` 为 200，HTTP 状态码为 201。

### 注册失败

| 场景 | HTTP 状态码 | 响应说明 |
|---|---|---|
| 用户名或密码不符合要求 | 400 | 返回具体校验错误信息 |
| 用户名已经存在 | 409 | `用户名已存在` |
| 密码 UTF-8 编码超过 72 字节 | 400 | `密码不能超过72字节` |

### 安全规则

- 密码使用 BCrypt 哈希处理，数据库不保存明文密码。
- 默认角色为 `USER`，默认状态为 `ACTIVE`。
- 注册成功只返回用户 ID、用户名和角色，不返回密码或密码哈希。
- 数据库对用户名设置唯一约束。

## 2. 用户登录

### 基本信息

- 请求方法：`POST`
- 请求路径：`/api/auth/login`
- 请求格式：`application/json`
- 接口状态：已实现

### 请求示例

```json
{
  "username": "pawfind_w2_test",
  "password": "TestPass123!"
}
```

以上为本地测试账号，仅供开发测试。

### 登录成功

HTTP 状态码：`200 OK`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "<JWT_TOKEN>",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

字段说明：

| 字段 | 含义 |
|---|---|
| `token` | JWT 登录凭证 |
| `tokenType` | Bearer |
| `expiresIn` | Token 有效期，单位为秒 |

### 登录失败

**用户名不存在或密码错误：HTTP 401**

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

**请求参数不合法：HTTP 400**

例如用户名或密码为空：

```json
{
  "code": 400,
  "message": "密码不能为空",
  "data": null
}
```

### 安全说明

1. 登录使用 BCrypt 验证密码，不直接比较明文密码与哈希字符串。
2. JWT 使用服务端密钥进行签名，有效期为 2 小时。
3. JWT 中只保存用户 ID 等必要声明，不保存密码。
4. 不在公开文档、日志或截图中展示真实 JWT。
5. 当前已实现登录与 JWT 签发，尚未实现受保护接口的 Token 校验和访问权限控制。

## 3. 开发状态

- 注册接口：已实现，并通过基本功能测试。
- 登录接口：尚未实现。
- 认证与权限控制：尚未实现，不应将当前接口直接部署为公开生产服务。