# 棍网球计分器部署

可上传部署包位于 `deploy/app-score`，可以整体手动上传到 Linux 服务器，不需要上传项目源代码。

## 目录内容

- `frond/dist`：由 `la-scoreboard-vue` 生成的前端文件。
- `back/lacrosse-scoreboard.jar`：Spring Boot 后端 JAR，Maven 构建、部署包和镜像内部统一使用该文件名。
- `Dockerfile`：将前端静态文件和 Java 后端制作成同一个镜像。
- `.env.test`：测试环境的数据库、Redis 和 JVM 配置。
- `deploy.sh`：使用 `back` 和 `frond` 构建前后端一体镜像，然后询问是否立即启动普通容器；选择“不启动”时保留镜像并直接结束。

镜像内部使用 Nginx 托管 `frond/dist`，并将 `/api` 转发到同容器的 Java `8080` 端口；镜像不携带单独的 `nginx.conf` 文件。容器只向服务器暴露一个 HTTP 端口，因此可以直接通过 `http://服务器IP:端口` 访问完整项目，也可以继续由服务器外层 Nginx 提供域名和 HTTPS。

Dockerfile 默认设置 `JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8`；使用普通容器启动时，`.env.test` 中的 `JAVA_OPTS` 可以覆盖该默认值。通过其他方式部署镜像时，也可以在容器环境变量中重新设置 JVM 参数。

MySQL 和 Redis 的连接地址、端口、账号等参数统一由 `.env.test` 注入，并在公共 `application.yml` 中读取；`application-dev.yml`、`application-test.yml` 和 `application-prod.yml` 只保存各环境不同的数据库及 Redis 连接池容量。修改 `.env.test` 后必须重新创建容器，已运行的容器不会自动读取文件变化。

## 上传前

在项目根目录使用 Git Bash 或 Linux/macOS 终端执行：

```bash
bash deploy/build-package.sh
```

`build-package.sh` 只在安装了 Node.js 和 Maven 的开发机执行：它会运行前端测试与构建、Java 测试与打包，然后更新 `deploy/app-score` 中的 `frond/dist` 和 `back/lacrosse-scoreboard.jar`。该脚本不构建或启动 Docker 镜像，服务器不需要上传或执行它。

## 服务器部署

1. 将整个 `deploy/app-score` 目录上传到服务器。服务器上的目标位置不限，不要求放在根目录。
2. 根据服务器实际情况修改 `.env.test`，尤其是数据库和 Redis 地址、账号、密码。
3. 进入上传后的 `app-score` 目录，授予部署脚本执行权限后运行：

```bash
chmod +x deploy.sh
./deploy.sh
```

例如上传到 `/opt/lacrosse/app-score` 时：

```bash
cd /opt/lacrosse/app-score
chmod +x deploy.sh
./deploy.sh
```

`deploy.sh` 根据自身所在目录查找 `Dockerfile`、`back/lacrosse-scoreboard.jar` 和 `frond/dist/index.html`，不依赖服务器上的固定绝对路径。执行时必须手动输入镜像版本号，例如输入 `0.1` 后，以整个 `app-score` 作为 Docker 构建上下文生成 `hub.lo/jx/lascore:0.1` 镜像；版本号为空或格式不合法时停止执行。

镜像构建完成后，脚本首先询问是否更新 Docker Service `msdev_lascore_dev`。输入 `y` 时实际执行：

```bash
docker service update --image hub.lo/jx/lascore:0.1 --force msdev_lascore_dev
docker service logs msdev_lascore_dev
```

必须通过 `--image` 指定刚构建的版本；只执行 `docker service update msdev_lascore_dev` 不会把服务切换到新版本。`--force` 保证重复使用同一版本号重新构建时也会滚动更新。脚本不会修改 Service 已有的数据库、Redis 等环境变量。

选择不更新 Service 后，脚本才会继续询问是否启动普通 Docker 容器。输入 `y` 时读取 `.env.test` 并更新普通容器；直接回车或输入 `n` 时只保留已经构建的镜像，不启动任何容器。

需要预先指定部署选择时可以使用以下方式，但镜像版本号仍会要求手动输入：

```bash
UPDATE_SERVICE=yes ./deploy.sh
UPDATE_SERVICE=no START_CONTAINER=yes ./deploy.sh
UPDATE_SERVICE=no START_CONTAINER=no ./deploy.sh
```

容器默认向服务器开放 `5173` 端口，访问地址为：

```text
http://服务器IP:5173
```

需要修改端口时可以临时指定：

```bash
APP_HOST_PORT=8088 ./deploy.sh
```

需要通过服务器外层 Nginx 提供域名时，只需把全部请求转发到该单一端口；容器内部会自行区分页面请求和 `/api` 请求。

选择更新 Service 时，脚本只更新 `msdev_lascore_dev` 并输出其日志；选择启动普通容器时，脚本只替换 `lacrosse-scoreboard` 容器；两项都选择不执行时，不修改任何现有容器。脚本不会删除 MySQL、Redis、服务器外层 Nginx 或其他项目容器。
