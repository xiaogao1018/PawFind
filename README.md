# PawFind - AI 社区宠物走失与智能寻回平台

## 项目简介

PawFind 是一个面向社区居民的宠物走失与智能寻回平台。

主要功能：
- 用户注册与登录
- 宠物走失信息发布
- 发现宠物上报与目击线索管理
- 社区发帖、评论和管理员审核
- AI 寻宠对话与寻宠启事生成

## 技术栈

### 前端
- Vue 3
- TypeScript
- Vite

### 后端
- Java 25
- Spring Boot 4.0.9
- Maven

### 后续集成
- MySQL
- Spring AI
- 大模型 API

## 项目结构

PawFind/
- frontend/：Vue 前端
- backend/：Spring Boot 后端
- docs/：项目需求与设计文档

## 本地启动

### 前端

cd frontend
npm install
npm run dev

默认访问：http://localhost:5173

### 后端

cd backend
./mvnw spring-boot:run

默认访问：http://localhost:8080

## 当前开发进度

W1D2：前后端项目初始化完成，业务功能尚未开发。


##  新终端
cd /Users/Admin/Documents/PawFind/backend
read -s "DB_PASSWORD?请输入 pawfind_app 的 MySQL 密码: "
export DB_PASSWORD
export JWT_SECRET="$(openssl rand -base64 32)"