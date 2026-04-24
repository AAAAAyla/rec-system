# AmazonRec - 智能电商推荐平台

一个基于 Spring Boot 3 + Vue 3 的全栈电商平台，集成 AI 智能推荐、RAG 检索增强生成、实时 IM 客服、多角色管理等功能。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| MyBatis | 3.0.4 | ORM 持久层 |
| MySQL | 8.x | 关系型数据库 |
| Redis | - | 缓存 / 接口限流 |
| Spring AI (智谱) | 1.0.0-M1 | AI 推荐 + RAG + 自动分类 |
| WebSocket | - | 实时 IM 通信 |
| Java-JWT | 4.4.0 | 身份认证 |
| BCrypt | - | 密码加密 |
| Knife4j | 4.4.0 | API 文档 |
| Lombok | 1.18.36 | 代码简化 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| Vite | 8.x | 构建工具 |
| Element Plus | 2.13 | UI 组件库 |
| Pinia | 3.x | 状态管理 |
| Vue Router | 4.6 | 路由管理 |
| Axios | 1.15 | HTTP 请求 |

## 项目结构

```
springquickstart/
├── src/main/java/com/ly/springquickstart/
│   ├── annotation/         # 自定义注解 (@RoleRequired)
│   ├── config/             # WebConfig、WebSocket、DatabaseInitializer
│   ├── controller/         # REST 控制器
│   ├── handler/            # WebSocket、JSON 类型处理器
│   ├── interceptor/        # 登录、角色、限流
│   ├── mapper/             # MyBatis Mapper
│   ├── pojo/               # 实体类
│   ├── scheduler/          # 定时任务
│   ├── service/            # 业务层（含 RagService 等）
│   └── exception/          # 全局异常
├── src/main/resources/
│   └── application.yaml
├── sql/
├── amazon-frontend/        # Vue 3 前端（独立子目录）
│   ├── src/api、components、router、store、views/
│   └── package.json
└── pom.xml
```

## 功能模块

### 三端角色

| 角色 | role 值 | 功能范围 |
|------|---------|----------|
| 买家 | 0 | 浏览、搜索、购物车、下单、评价、收藏、IM、AI 助手 |
| 商家 | 1 | 商品、订单、发货、仓库、优惠券、IM、数据看板 |
| 管理员 | 2 | 用户、商家审核、分类、商品与订单、统计 |

### 买家端（节选）

首页分类与商品流、搜索、商品详情（SKU）、购物车、结算、订单全流程、收货地址、收藏、IM、通知、个人资料。

### 商家端（节选）

数据看板（含饼图）、商品管理（含 AI 自动分类）、订单与发货、仓库、优惠券、店铺、IM。

### 管理员端（节选）

统计、用户、商家审核、分类 CRUD、全量商品与订单。

### AI 助手

RAG + 意图识别、商品推荐、优惠券如实回答、上架时自动分类、可拖拽悬浮窗与快捷问题。

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

## 快速启动

### 1. 数据库

```sql
CREATE DATABASE rec_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后端时 `DatabaseInitializer` 会尝试自动建表与初始化数据（以项目代码为准）。

### 2. 配置

编辑 `src/main/resources/application.yaml`：MySQL 连接、Redis、智谱 `spring.ai.zhipuai.api-key`。

### 3. 后端

```bash
cd springquickstart
mvn spring-boot:run
```

默认：`http://localhost:8080`

### 4. 前端

```bash
cd springquickstart/amazon-frontend
npm install
npm run dev
```

默认：`http://localhost:5173`

前端 API 地址见 `amazon-frontend/.env.development` 中的 `VITE_API_BASE`。

### 5. 默认管理员（若已由初始化逻辑创建）

| 账号 | 密码 | 角色 |
|------|------|------|
| platform_admin | admin123 | 管理员 |

## API 文档

启动后端后：`http://localhost:8080/doc.html`（Knife4j）

## 安全机制（概要）

JWT、`Authorization` 头、BCrypt 密码、`@RoleRequired` + 角色拦截、Redis 限流、登录白名单。

## 说明：根目录 README 为何会变成 Vue 模板？

若本文件只剩「Vue 3 + Vite」几行，通常是：

- 在仓库根目录误用 `npm create vite` 覆盖了 `README.md`，或
- 把 `amazon-frontend` 里 Vite 自带的 README 复制到了项目根目录。

本仓库应以**本文件（AmazonRec 全栈说明）**为准；前端子项目若需单独说明，可放在 `amazon-frontend/README.md`。
