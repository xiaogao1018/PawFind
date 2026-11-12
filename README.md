# PawFind

PawFind 是面向社区居民的宠物走失与寻回平台。现有页面支持注册登录、走失与发现记录、走失图片、目击线索、社区帖子和评论，以及管理员审核、账号管理和操作日志。

## 本地启动

需要 Java 25、MySQL 8.4、Node.js 和 npm。后端通过 Maven Wrapper 构建。

1. 准备 MySQL 数据库和账号。新建空的 `pawfind` 数据库后，使用有建表权限的账号导入 `database/pawfind.sql`。已有数据库若仍使用旧版表结构，按顺序各执行一次 `database/migrations/001_page_api.sql` 、`database/migrations/002_profiles_and_images.sql` 和 `database/migrations/003_management_and_messages.sql`。已执行 002 的数据库仅执行 003。初始化脚本包含删表语句，请仅用于空库。
2. 让后端账号 `pawfind_app` 可以读写 `pawfind` 库。在后端终端设置数据库密码和 JWT 密钥，再启动服务。

   ```zsh
   cd backend
   read -rs "DB_PASSWORD?MySQL 密码: "
   export DB_PASSWORD
   export JWT_SECRET="$(openssl rand -base64 32)"
   ./mvnw spring-boot:run
   ```

   后端默认监听 `http://localhost:8080`，上传图片默认保存在 `backend/uploads`。已有图片需要同时保留附件记录及对应磁盘文件。

3. 在另一个终端启动前端。

   ```sh
   cd frontend
   npm ci
   npm run dev
   ```

   前端默认访问 `http://localhost:5173`。

## 初始化管理员

先在前端注册一个专用账号，再使用有权限的 MySQL 账号执行以下语句，把 `your_admin_username` 改成实际用户名。

```sql
UPDATE sys_user SET role = 'ADMIN' WHERE username = 'your_admin_username';
```

刷新页面后可进入管理后台。账号角色和禁用状态以数据库中的当前值为准。

## 页面与图片更新

登录后进入首页。用户中心提供宠物档案增删改、个人走失与发现记录、社区动态及退出登录。社区提供分享、求助、问答和知识分类，支持配图。走失和发现列表显示首张照片。

更新已有环境时，执行尚未应用的迁移脚本后重启后端。前端开发服务会自动刷新代码，刷新页面即可查看更新。

## 管理与消息

管理员登录后进入独立管理平台，可管理走失、招领、社区帖子和评论、用户账号，并查询操作日志。普通用户导航包括走失发布、招领发布、社区互动、消息中心和用户中心。

通知从 003 迁移后的新互动开始生成。消息中心接收走失或招领留言、寻宠线索、社区评论、审核和管理通知，支持按用户名或从详情页发起私信。消息页会定期同步新消息，只有对话双方可以读取私信。

AI 对话界面已提供流式读取、停止回复和错误处理。当前服务端返回尚未开放的提示。后续接入约定见 `docs/api/messages-and-ai-api.md`。

## 接口行为

- 走失和发现的公开列表及详情支持游客浏览。写入需要登录，编辑和撤回由发布者操作。
- 目击线索和联系方式仅对走失记录发布者、对应线索提交者和管理员可见。只有走失记录发布者可以核实线索。
- 社区需要登录。作者可在「我的帖子」查看审核状态及原因。新帖和修改后的帖子需重新审核，通过且可见的帖子才进入社区列表并开放评论。
- 管理员可审核帖子、隐藏或恢复已通过的帖子，以及禁用或恢复账号。状态变更与操作日志在同一事务中保存。

AI 寻宠对话以及需求文档中尚无对应页面的功能，暂未接入本次联调。
