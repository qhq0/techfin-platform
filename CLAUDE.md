# techfin-platform

CCB TechFin Platform — 科技金融平台。

## Project Structure

```
techfin-platform/
├── pom.xml                     # 父 POM：版本管理、模块聚合
├── CLAUDE.md
├── docs/                       # 文档
├── techfin-common/             # 【公共模块】（无启动类）
│   └── src/main/java/com/ccb/techfin/common/
│       ├── api/                # 统一响应包装（CommonResp）
│       ├── exception/          # 全局异常处理 + 业务异常类
│       ├── utils/              # 全局工具类
│       └── config/             # 全局配置（如 Redis、MyBatis）
│
└── techfin-app/               # 【主业务模块】（含启动类）
    ├── pom.xml                # 依赖 techfin-common
    └── src/main/java/com/ccb/techfin/
        ├── TechfinApplication.java    # 唯一的启动类
        │
        └── sxd/                      # 【善新贷业务域】（垂直分层）
            ├── controller/
            │   └── MaterialUploadController.java
            ├── service/
            │   ├── MaterialUploadService.java
            │   └── impl/MaterialUploadServiceImpl.java
            ├── mapper/
            │   └── ApplicationRecordRepository.java
            ├── entity/
            │   └── ApplicationRecord.java
            ├── dto/
            │   ├── request/
            │   ├── response/
            │   └── external/
            ├── enums/
            ├── config/
            └── validator/
```

## Build & Run

```bash
# 编译整个平台
mvn clean compile

# 运行主业务模块（需要 MySQL localhost:3306/mydb）
mvn spring-boot:run -pl techfin-app
```

## Key Conventions

- **Package:** `com.ccb.techfin.*` — `common` 放公共基础类，`sxd` 放善新贷业务
- **API路径:** `/techfin` (context-path) + `/sxd` (controller RequestMapping)
- **响应格式:** `CommonResp<T>` — `code=0` 成功，`code=-1` 错误
- **Jackson:** `SNAKE_CASE`
- **异常码:** 使用String类型业务码

## Module Dependencies

```
techfin-app ──> techfin-common
```

`TechfinApplication` 使用 `@SpringBootApplication(scanBasePackages = "com.ccb.techfin")` 扫描两个模块的 Bean。
