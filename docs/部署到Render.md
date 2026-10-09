# 部署到 Render（后端 + 前端同一个网址）

前端构建产物会打包进 Spring Boot 的 jar，由后端一起托管，所以最终只有一个网址，没有跨域问题。

- 应用：Render 免费 Web Service（Java 运行时）
- 数据库：TiDB Cloud Serverless 免费 MySQL 兼容库（Render 免费套餐不提供 MySQL）

---

## 一、准备数据库（TiDB Cloud Serverless）

1. 打开 https://tidbcloud.com 注册登录（可用邮箱或 GitHub，不需要信用卡）。
2. 左侧 **Clusters** → **Create Cluster** → 选择 **Serverless** → 区域选一个离你近的（例如 `AWS / Singapore` 或 `AWS / Tokyo`）→ 创建。
3. 等集群状态变成 **Active**，点 **Connect**，记下这几项：
   - `Host`，形如 `gateway01.ap-southeast-1.prod.aws.tidbcloud.com`
   - `Port`，一般是 `4000`
   - `User`，形如 `3Kx8AbcDEfGh.root`（**注意带 `.root` 后缀，要整段复制**）
   - `Password`：点 **Generate Password** 生成后立刻复制保存，关掉就看不到第二次
4. 建库这一步**不用手动做**：应用连接串带了 `createDatabaseIfNotExist=true`，
   第一次启动会自动建 `study_assistant` 库、建表并写入示例数据。

> 如果你更想用别的免费 MySQL（Aiven、Clever Cloud 等）也可以，下面所有配置方式完全一样。

---

## 二、在 Render 建 Web Service

1. 打开 https://dashboard.render.com → **New** → **Web Service**。
2. 连接你的 GitHub 仓库 `laai97232-eng/ai-and-knowleage`。
3. 按下表填写：

| 配置项 | 值 |
|---|---|
| Name | `study-assistant`（随意，会决定网址前缀） |
| Language / Runtime | `Java` |
| Region | 选和数据库同一区域，例如 `Singapore` |
| Branch | `main` |
| **Root Directory** | `backend` |
| Build Command | `mvn -B -DskipTests clean package` |
| Start Command | `java -Dfile.encoding=UTF-8 -jar target/study-assistant-1.0.0.jar` |
| Instance Type | `Free` |

4. 展开 **Advanced** → **Add Environment Variable**，逐个添加：

| Key | Value |
|---|---|
| `MYSQL_HOST` | TiDB 的 Host |
| `MYSQL_PORT` | `4000` |
| `MYSQL_DATABASE` | `study_assistant` |
| `MYSQL_USERNAME` | TiDB 的 User（**含 `.root` 后缀**） |
| `MYSQL_PASSWORD` | TiDB 生成的密码 |
| `JWT_SECRET` | 随便一串 32 位以上的字符，例如 `study-assistant-prod-secret-change-me-123456` |

可选（不填也不影响运行）：

| Key | 说明 |
|---|---|
| `AI_BASE_URL` | 例如 `https://api.deepseek.com`，填了才启用真实大模型问答 |
| `AI_API_KEY` | 大模型密钥 |
| `AI_CHAT_MODEL` | 默认 `deepseek-chat` |
| `APP_UPLOAD_DIR` | 上传目录，免费实例没有持久磁盘，上传的文件重启会丢 |

> `PORT` 不用配，Render 会自动注入，`application.yml` 里已经写成 `${PORT:8080}`。

5. 点 **Create Web Service**，等构建 + 部署完成。
   第一次启动因为要连数据库、建表、灌示例数据，大概需要 1～2 分钟。

6. 打开 Render 给的网址，形如 `https://study-assistant-xxxx.onrender.com`，
   用 `admin / 123456` 或 `student / 123456` 登录。

---

## 三、以后怎么更新

改了代码之后，在本地执行：

```powershell
# 1. 重新构建前端并同步到后端静态资源
powershell -ExecutionPolicy Bypass -File scripts\build-web.ps1

# 2. 提交推送，Render 会自动重新部署
git add -A
git commit -m "更新内容"
git push
```

也可以直接跑 `scripts\deploy.ps1`，它把这两步合成一步。

---

## 四、常见问题

**页面能打开但数据全是空的 / 报「无法连接服务器」**
看 Render 的 **Logs**。如果出现 `Access denied` 或 `Unknown database`，
八成是 `MYSQL_USERNAME` 漏了 `.root` 后缀，或者密码复制错了。

**首次访问很慢（十几秒）**
免费实例 15 分钟没有请求会休眠，下次访问要冷启动；TiDB Serverless 空闲也会暂挂。
两者叠加，第一次可能要等 20 秒左右，之后就正常了。

**上传的 PDF 重启后不见了**
免费实例的磁盘是临时的。想保留就升级 Render 并挂载持久磁盘，
然后把 `APP_UPLOAD_DIR` 指到磁盘挂载点（例如 `/data/uploads`）。
示例资料本身写在 `catalog.json` 里，每次启动都会重新生成，不受影响。

**想改回前后端分开部署**
把前端单独发到 GitHub Pages 时，构建前设置环境变量
`VITE_API_BASE=https://study-assistant-xxxx.onrender.com`，
并在 Render 加一项 `APP_ALLOWED_ORIGINS=https://laai97232-eng.github.io` 放行跨域。
