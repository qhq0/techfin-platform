# techfin-platform

CCB TechFin Platform — 科技金融平台。

## Project Structure

```
techfin-platform/
├── pom.xml                     # 父 POM：版本管理、模块聚合
├── CLAUDE.md
├── docs/                       # 文档
├── techfin-common/             # 公共模块：统一响应、基础异常、全局异常处理
│   └── src/main/java/com/ccb/techfin/common/
│       ├── GlobalExceptionHandler.java
│       ├── dto/response/
│       │   ├── CommonResp.java
│       │   └── ApiResponse.java
│       └── exception/
│           ├── BusinessException.java
│           └── FileValidationException.java
└── techfin-sxd/                # 善新贷微服务
    └── src/main/java/com/ccb/techfin/sxd/
        ├── SxdApplication.java           # Spring Boot 启动类
        ├── config/
        │   ├── ApiProperties.java
        │   ├── FileUploadConfig.java
        │   └── RestTemplateConfig.java
        ├── controller/
        │   └── MaterialUploadController.java
        ├── dto/
        │   ├── request/UploadMaterialsRequest.java
        │   ├── response/UploadMaterialsResponse.java
        │   └── external/ (AttachmentUploadResponse, DocBatchAddItem, DocBatchAddResponse)
        ├── entity/
        │   └── ApplicationRecord.java
        ├── enums/
        │   └── TaskStatus.java
        ├── repository/
        │   └── ApplicationRecordRepository.java
        ├── service/
        │   ├── MaterialUploadService.java
        │   └── impl/MaterialUploadServiceImpl.java
        ├── validator/
        │   └── FileValidator.java
        └── resources/
            └── application.properties
```

## Build & Run

```bash
# 编译整个平台
mvn clean compile

# 运行善新贷微服务（需要 MySQL localhost:3306/mydb）
mvn spring-boot:run -pl techfin-sxd
```

## Key Conventions

- **Package:** `com.ccb.techfin.*` — `common` 放公共基础类，`sxd` 放善新贷业务
- **API路径:** `/techfin` (context-path) + `/sxd` (controller RequestMapping)
- **响应格式:** `CommonResp<T>` — `code=0` 成功，`code=-1` 错误
- **Jackson:** `SNAKE_CASE`
- **异常码:** 使用String类型业务码

## Module Dependencies

```
techfin-sxd ──> techfin-common
```

`SxdApplication` 使用 `@SpringBootApplication(scanBasePackages = "com.ccb.techfin")` 扫描两个模块的 Bean。
