# 智能学习助手

第一阶段是学习平台基础系统：用户、课程、知识点、资料、题库和学习记录。

第二阶段在同一套系统上增加资料解析、向量检索和 AI 问答。学生可以按课程提问，回答会带来源。向量存在 MySQL 里，不需要单独的向量库。没有配置大模型密钥时，系统按检索到的资料原文摘录回答。

第三阶段在同一套系统上增加知识图谱、学习路径、薄弱点分析和图表。图谱和路径使用课程里的知识点关系：实线表示前置，虚线表示相关。薄弱点按错题次数和做题正确率计算，正确率低于 60% 或仍有错题的知识点会被标出来。本机没有单独的图数据库，关系存在 MySQL 里，页面用图表直接画出这张图。

## 环境

- JDK 17（本机编译使用 JDK 17，接口按 Spring Boot 3 编写）
- Maven 3.9
- Node.js 20+
- MySQL 8，库名 `study_assistant` 会在首次启动时自动建表

默认数据库账号写在 `backend/src/main/resources/application.yml`，可用环境变量覆盖：

- `MYSQL_USERNAME`，默认 `root`
- `MYSQL_PASSWORD`，默认 `123456`

可选的大模型（OpenAI 兼容接口，例如 DeepSeek）：

- `AI_BASE_URL`，例如 `https://api.deepseek.com`
- `AI_API_KEY`
- `AI_CHAT_MODEL`，默认 `deepseek-chat`

不填这三项时，问答走本地摘录，不请求外部模型。管理员可在「资料管理」里重建索引。

## 启动

```bash
# 后端
set JAVA_HOME=C:\JDK\jdk-17_windows-x64_bin\jdk-17.0.12
mvn -f backend/pom.xml spring-boot:run

# 前端
cd frontend
npm install
npm run dev
```

https://ai-and-knowleage.vercel.app/

浏览器打开 http://localhost:5173

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | admin | 123456 |
| 学生 | student | 123456 |

示例数据以《Java程序设计》为主，另外有数据库、操作系统、计算机网络、数据结构、软件工程的课程目录。

如果要重新灌入示例数据，先清空库再启动：

```sql
DROP DATABASE study_assistant;
CREATE DATABASE study_assistant DEFAULT CHARACTER SET utf8mb4;
```
