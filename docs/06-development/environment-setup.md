# 开发环境基线

## 必需软件

- Git
- Java 17
- Maven 3.9 或兼容版本
- Node.js LTS
- npm
- MySQL 8

## 可选软件

- Redis
- Docker Desktop
- IntelliJ IDEA
- Visual Studio Code

## 环境检查命令

```powershell
git --version
java -version
mvn -version
node --version
npm --version
mysql --version
docker --version
```

Codex 必须记录每条命令的实际输出。缺失依赖时先说明根因和安装要求，不得假装已经创建可运行工程。

## 预定端口

- 后端：`8080`
- 前端开发服务器：`5173`
- MySQL：`3306`
- Redis：`6379`

如端口被占用，应记录实际调整，保证前后端配置一致。
