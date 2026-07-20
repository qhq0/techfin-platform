# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# 编译整个平台
mvn clean compile

# 安装到本地仓库（供模块间引用）
mvn install -DskipTests

# 运行（需要 MySQL localhost:3306/mydb）
mvn spring-boot:run -pl techfin-controller

# 仅编译特定模块
mvn compile -pl techfin-service -am
```

## Tech Stack

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.11 |
| MyBatis-Plus | 3.5.16 (取代 JPA) |
| MySQL | 8.0+ (InnoDB, utf8mb4) |
| Lombok | 项目标配 |
| Jackson | 全局 SNAKE_CASE |

## Module Dependency Chain

```
techfin-controller ──> techfin-service ──> techfin-dao ──> techfin-model
         │                                 │
         └──> techfin-common               └──> techfin-common (via service)
```

五个 Maven 模块，均为 com.ccb 的子模块，聚合在父 POM `techfin-platform` 下。

## Package Map

| 模块 | 基包 | 职责 |
|------|------|------|
| `techfin-common` | `com.ccb.techfin.common` | `result/CommonResp`、`exception/BusinessException`、`GlobalExceptionHandler` |
| `techfin-model` | `com.ccb.techfin.model.sxd` | Entity、DTO、Enum（无业务逻辑） |
| `techfin-dao` | `com.ccb.techfin.dao.sxd` | MyBatis-Plus Mapper（`extends BaseMapper<T>`） |
| `techfin-service` | `com.ccb.techfin.service.sxd` | Service 接口+实现、Config、Validator |
| `techfin-controller` | `com.ccb.techfin.controller.sxd` | REST Controller + `TechfinApplication` 启动类 |

## Key Conventions

### 1. 统一响应 — `CommonResp<T>`

```java
CommonResp.success(data);                    // code=0, msg="成功"
CommonResp.success("操作成功", data);         // code=0
CommonResp.error(-1, "错误信息");             // 业务异常
```

- `code=0` 成功，`code=-1` 错误（业务异常统一返回 -1，前端按 code 判断）

### 2. 异常体系

- **`BusinessException(code, message)`** — 业务层抛出，`GlobalExceptionHandler` 捕获返回 `400`。`code` 是 String 类型业务码（如 `PARAM_MISSING`、`ATTACH_NOT_FOUND`）
- **`FileValidationException`** — 继承自 `BusinessException`，文件校验专用
- 全局异常处理统一返回 `CommonResp.error(-1, e.getMessage())`

### 3. Jackson SNAKE_CASE

全局配置 `spring.jackson.property-naming-strategy=SNAKE_CASE`，导致：

- **请求体 DTO 必须显式覆盖**：用 `@JsonNaming(LowerCamelCaseStrategy.class)` 标注前端传 camelCase 字段的 DTO（如 `SubmitMaterialsRequest`、`SubmitFileItem`）
- **外部 API 请求也必须覆盖**：对外部 API 发送的 DTO 需要 `@JsonNaming(LowerCamelCaseStrategy.class)`，否则会序列化为 snake_case 导致外部 API 不识别（如 `DocBatchAddItem`）
- **响应自动转 snake_case**：Controller 返回的 `CommonResp` 中字段名会自动转换

### 4. MyBatis-Plus 模式

- 实体类用 `@TableName`、`@TableId`、`@TableField` 注解
- `@TableId(type = IdType.AUTO)` 自增主键，`@TableId(type = IdType.INPUT)` 手工赋值主键
- 枚举实现 `IEnum<String>`，`getValue()` 返回 `name()`，数据库存枚举常量名（如 `TaskStatus.PENDING_ANALYSIS` → `"PENDING_ANALYSIS"`）
- Mapper 接口 `@Mapper` + `extends BaseMapper<T>`，无自定义方法时为空接口
- 动态查询用 `LambdaQueryWrapper<T>`（如 `new LambdaQueryWrapper<ApplicationAttachment>().eq(...)`）
- 删除用 `mapper.delete(new LambdaQueryWrapper<>()...eq(...))`
- 无需 `@EntityScan`/`@MapperScan`，`@SpringBootApplication(scanBasePackages = "com.ccb.techfin")` 扫描所有模块

### 5. 事务管理

所有写操作 Service 方法加 `@Transactional(rollbackFor = Exception.class)`：
- `uploadFile()` — 上传文件 + 写入 application_att
- `submitMaterials()` — 创建申请记录 + 批量新增 + 写入 application_doc + 清理 application_att
- `confirmControllerName()` — 更新 application_record
- `deleteAttachment()` — 删除 application_att 记录

### 6. 外部 API 调用模式

通过 `RestTemplate` 调用外部接口，响应统一用 `ExternalResponse` 包装：

```java
// 通用响应类
ExternalResponse { boolean success; String code; String message; Object data; }
// data 转换
respBody.getDataAs(DocBatchAddData.class);
```

关键校验步骤：`respBody == null` → 抛异常 → `!respBody.isSuccess()` → 抛异常（对外部 API data 可能还需要 `respBody.getData() == null` 判断）。

### 7. API 路径

Context-path: `/techfin`
Controller base: `/sxd`

完整路径示例：
- `POST /techfin/sxd/upload-attachment` — 上传附件
- `DELETE /techfin/sxd/delete-attachment/{att_id}` — 删除附件
- `POST /techfin/sxd/submit-materials` — 提交资料
- `GET /techfin/sxd/controller-name/{customer_no}` — 查询实控人
- `PUT /techfin/sxd/application-record/controller-name` — 确认实控人

## Database Tables

| 表名 | 主键 | 说明 |
|------|------|------|
| `application_att` | `id` (BIGINT AUTO_INCREMENT) | 附件元信息，`att_id` 唯一索引 |
| `application_record` | `task_id` (VARCHAR(64)) | 申请记录，手工生成 `TASK-<32位hex>` |
| `application_doc` | `doc_id` (VARCHAR(64)) | 文档明细，外部 API 返回的 ID |
| `sxd_profile` | `cst_id` (VARCHAR(200)) | 客户信息表，以 `cst_id` 为主键 |

详见 `docs/init-tables.sql`。

## Configuration

配置文件统一集中在 `techfin-controller/src/main/resources/application.properties`，主要包括：
- `api.doc-type.finance` / `api.doc-type.business` — 文档类型 ID 映射
- `file.upload.allowed-extensions.*` — 不同业务类型的文件扩展名白名单
- `api.default-token` — 外部 API 鉴权 token
- `mybatis-plus.configuration.log-impl` — SQL 日志

配置类：`ApiProperties`（prefix=`api`）、`FileUploadConfig`（prefix=`file.upload`）

## Documentation

业务功能说明文档在 `docs/` 目录下：
- `docs/上传材料功能说明.md` — 附件上传 + 提交资料全流程
- `docs/客户信息查询接口说明.md` — 实控人查询 + 确认 API
- `docs/init-tables.sql` — 建表 SQL
- `docs/api.json` — Postman Collection
