# techfin-platform

CCB TechFin Platform — 科技金融平台。

## Module Dependency Chain

```
techfin-controller ──> techfin-service ──> techfin-dao ──> techfin-model
         │                                 │
         └──> techfin-common               └──> techfin-common (via service)
```

## Project Structure

```
techfin-platform/
├── pom.xml                                 # 父 POM：版本管理、模块聚合（packaging=pom）
├── CLAUDE.md
├── docs/                                   # 文档

├── techfin-common/                         # 【公共模块】（library jar）
│   ├── pom.xml                             # 依赖：spring-boot-starter-web、lombok
│   └── src/main/java/com/ccb/techfin/common/
│       ├── result/                         # 统一响应包装 CommonResp<T>
│       ├── exception/                      # BusinessException、FileValidationException、GlobalExceptionHandler
│       ├── utils/                          # 全局工具类
│       └── config/                         # 全局配置（Redis、MyBatis 等）

├── techfin-model/                          # 【模型层】（library jar）
│   ├── pom.xml                             # 依赖：spring-boot-starter-data-jpa、lombok、jackson
│   └── src/main/java/com/ccb/techfin/model/sxd/
│       ├── entity/                         # JPA 实体（ApplicationRecord）
│       ├── dto/request/                    # 请求 DTO
│       ├── dto/response/                   # 响应 DTO
│       ├── dto/external/                   # 外部 API 响应 DTO
│       ├── enums/                          # 枚举（TaskStatus）
│       └── vo/                             # 值对象

├── techfin-dao/                            # 【数据访问层】（library jar）
│   ├── pom.xml                             # 依赖：techfin-model、spring-boot-starter-data-jpa、mysql
│   └── src/main/
│       ├── java/com/ccb/techfin/dao/sxd/
│       │   └── SxdMapper.java             # JPA Repository
│       └── resources/mapper/sxd/           # MyBatis XML（可选）

├── techfin-service/                        # 【业务逻辑层】（library jar）
│   ├── pom.xml                             # 依赖：techfin-dao、techfin-model、techfin-common
│   └── src/main/java/com/ccb/techfin/service/sxd/
│       ├── SxdService.java                 # 业务接口
│       ├── impl/SxdServiceImpl.java        # 业务实现
│       ├── config/                         # 业务配置（ApiProperties、FileUploadConfig、RestTemplateConfig）
│       └── validator/                      # 校验器（FileValidator）

└── techfin-controller/                     # 【Web 层 / 启动模块】（可执行 jar）
    ├── pom.xml                             # 依赖：techfin-service、techfin-common
    │                                       # 含 spring-boot-maven-plugin（mainClass=TechfinApplication）
    └── src/main/
        ├── java/com/ccb/techfin/
        │   ├── TechfinApplication.java     # 唯一的启动类
        │   └── controller/sxd/
        │       └── SxdController.java      # REST Controller
        └── resources/
            └── application.properties      # 全部配置文件
```

## Build & Run

```bash
# 编译整个平台
mvn clean compile

# 运行（需要 MySQL localhost:3306/mydb）
mvn spring-boot:run -pl techfin-controller
```

## Key Conventions

- **Package:** `com.ccb.techfin.*` — 按 layer 分包：`controller`、`service`、`dao`、`model`，common 放公共基础类
- **API路径:** `/techfin` (context-path) + `/sxd` (controller RequestMapping)
- **响应格式:** `CommonResp<T>` — `code=0` 成功，`code=-1` 错误
- **Jackson:** `SNAKE_CASE`
- **异常码:** 使用 String 类型业务码

`TechfinApplication` 使用 `@SpringBootApplication(scanBasePackages = "com.ccb.techfin")` 扫描所有模块的 Bean。
