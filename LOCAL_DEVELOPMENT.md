# 本地开发（Windows）

需要 JDK 17、Maven 3.9、Node.js 20、MySQL Server 8、Redis（可使用 Memurai）和 RocketMQ 5.2.0。`local` profile 不使用 Nacos，网关直接连接聚合服务。

## 首次准备

1. 设置 `JAVA_HOME` 为安装的 JDK 17 路径，并确保 `mvn` 在 PATH 中。
2. 在项目根目录创建 `.local`。MySQL 使用单独的 `.local/mysql-data`，不复制或上传实际数据。
3. 初始化一个新的空数据目录，启动 MySQL 3307，设置本地 root 密码为 `root`，创建 `link` 数据库并导入结构与开发 fixture。若目录已经存在，不要重新初始化或重导建表 SQL。

```powershell
# 以下用于全新的本地目录；替换为你实际安装的 MySQL 路径。
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$dataDir = "$PWD\.local\mysql-data"
& "$mysqlBin\mysqld.exe" --no-defaults --initialize-insecure "--datadir=$dataDir"
Start-Process -FilePath "$mysqlBin\mysqld.exe" -ArgumentList @('--no-defaults', "--datadir=$dataDir", '--port=3307', '--bind-address=127.0.0.1', '--mysqlx=0') -WindowStyle Hidden
# 等数据库就绪；初始化阶段 root 尚无密码。
& "$mysqlBin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; CREATE DATABASE link CHARACTER SET utf8mb4;"
& "$mysqlBin\mysql.exe" -h 127.0.0.1 -P 3307 -u root --password=root link -e "source resources/database/link.sql; source resources/database/link-data.sql;"
```

4. 启动 Redis 6379，配置示例密码 `123456`。本地脚本不会停止共享的 Memurai 服务。
5. 将 RocketMQ 5.2.0 解压到 `.local/tools/rocketmq-all-5.2.0-bin-release`，先启动 nameserver 9876，再启动 broker 10911：

```powershell
$rmq = "$PWD\.local\tools\rocketmq-all-5.2.0-bin-release"
$env:ROCKETMQ_HOME = $rmq
Start-Process -FilePath "$rmq\bin\mqnamesrv.cmd" -WorkingDirectory $rmq -WindowStyle Hidden
# 等待 9876 就绪；本地 broker 配置由脚本生成，不固定盘符。
$storePath = ("$PWD\.local\rocketmq-store" -replace '\\', '/')
New-Item -ItemType Directory -Path .local/rocketmq-store -Force | Out-Null
@"
brokerClusterName=DefaultCluster
brokerName=broker-local
brokerId=0
brokerIP1=127.0.0.1
namesrvAddr=127.0.0.1:9876
listenPort=10911
storePathRootDir=$storePath
storePathCommitLog=$storePath/commitlog
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
flushDiskType=ASYNC_FLUSH
"@ | Set-Content .local/rocketmq-broker-local.conf -Encoding ascii
Start-Process -FilePath "$rmq\bin\mqbroker.cmd" -ArgumentList @('-c', "$PWD\.local\rocketmq-broker-local.conf") -WorkingDirectory $rmq -WindowStyle Hidden
```

nameserver 地址可通过 `ROCKETMQ_NAME_SERVER` 覆盖。启动脚本不管理 RocketMQ，启动应用前必须确认消息服务已就绪。

## 构建与启动

```powershell
mvn -B package "-DskipTests" "-Dspotless.apply.skip=true"
Push-Location console-vue
npm ci
Pop-Location
.\scripts\start-local.ps1
```

入口：http://127.0.0.1:5173；账号 `admin / admin123456`。后端健康检查：http://127.0.0.1:8003/actuator/health。

`.\scripts\stop-local.ps1` 停止应用和本地 MySQL，保留共享 Redis；RocketMQ 需另行停止。复制包含数据库的本地项目之前，先停止写入服务。

## 开发配置与限制

- 示例用户名、口令和 AES key 仅用于本地，不应用于生产。生产配置模板见各模块 `.yaml.example`。
- 可选 `$env:AMAP_KEY = '你的地图 key'`；缺省时不查地域，基础访问统计仍写入。
- 空 Redis 不含既有短链的布隆标记；测试跳转时应先通过创建接口新建短链。
- 修改后端后需重新打包。运行中的 jar 可能被 Windows 锁定，打包前先停止该应用。
- 项目源码不包含 `.local` 数据、中间件发行包、`node_modules`、`target` 或内部笔记，需要按说明重新准备。
