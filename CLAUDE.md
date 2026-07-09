# techfin-platform

This is the parent project for the TechFin Platform. It contains the following modules:

- **shanxindai** (善新贷) — enterprise material upload and information collection service (aggregator POM)
  - **shanxindai-common** — config, DTO, exception, validator
  - **shanxindai-model** — domain entities, enums, repository interfaces
  - **shanxindai-service** — service interfaces and business logic
  - **shanxindai-controller** — web controllers, global exception handler, application entry point

## Build & Run

```bash
# Build the entire platform (from root)
mvn clean compile

# Build only the shanxindai modules
mvn clean compile -pl shanxindai

# Build a specific submodule
mvn clean compile -pl shanxindai/shanxindai-service

# Package as JAR (from root, builds all modules)
mvn clean package -DskipTests

# Run the shanxindai module locally (requires MySQL on localhost:3306)
mvn spring-boot:run -pl shanxindai/shanxindai-controller

# Run from within the controller directory
cd shanxindai/shanxindai-controller && ../../mvnw spring-boot:run
```

The app runs on `http://localhost:8080/techfin`. Requires MySQL database `mydb` accessible with user `qiu`. Database tables are auto-created via JPA `ddl-auto=update`.

## Project Overview

**善新贷 (ShanXinDai)** — a Spring Boot 3.5 service for enterprise material upload and information collection. It accepts multipart file uploads from clients, forwards files to downstream external APIs (attachment upload + doc batch creation), and persists application records in MySQL.

**Tech stack:** Java 17, Spring Boot 3.5.11, Spring Data JPA, MySQL, Lombok, Maven.

## Module Structure

```
techfin-platform/
├── pom.xml                           # Parent POM (packaging: pom)
├── CLAUDE.md
├── docs/                             # Documentation
└── shanxindai/                       # Aggregator POM
    ├── pom.xml
    ├── shanxindai-common/            # Config, DTO, exception, validator
    ├── shanxindai-model/             # JPA entities, enums, repositories
    ├── shanxindai-service/           # Service interfaces & implementations
    └── shanxindai-controller/        # Controllers, exception handler, main class
```

### Module Dependencies

```
shanxindai-controller  ──>  shanxindai-service  ──>  shanxindai-model
                              │
                              └──>  shanxindai-common
                                      ↑
shanxindai-controller ────────────────┘
```

### Module: shanxindai-common (com.example.shanxindai.*)
- `config/` — ApiProperties, FileUploadConfig, RestTemplateConfig
- `dto/request/` — UploadMaterialsRequest
- `dto/response/` — CommonResp, ApiResponse, UploadMaterialsResponse
- `dto/external/` — AttachmentUploadResponse, DocBatchAddItem, DocBatchAddResponse
- `exception/` — BusinessException, FileValidationException
- `validator/` — FileValidator

### Module: shanxindai-model (com.example.shanxindai.*)
- `entity/` — ApplicationRecord (JPA entity)
- `enums/` — TaskStatus, FileType
- `repository/` — ApplicationRecordRepository

### Module: shanxindai-service (com.example.shanxindai.*)
- `service/` — MaterialUploadService interface
- `service/impl/` — MaterialUploadServiceImpl (core business logic)

### Module: shanxindai-controller (com.example.shanxindai.*)
- `controller/` — MaterialUploadController (POST /sxd/upload-materials)
- `GlobalExceptionHandler` — @RestControllerAdvice
- `ShanxindaiApplication` — Spring Boot main class

### Request Pipeline (shanxindai-service: MaterialUploadServiceImpl)

1. **Parameter validation** — credit code (18-char alphanumeric), customer number
2. **Uniquity check** — `existsByCreditCode()` prevents duplicate submissions
3. **File validation** — size limit (50MB default), extension whitelist per business type (`finance`/`business`), MIME type check
4. **Attachment upload** — POST to `/api/mdm/open/att/upload` per file (multipart), returns `attId`
5. **Doc batch add** — POST to `/api/extract/open/doc/batch/add` with all uploaded attIds, returns `docId`s
6. **Persist** — save `ApplicationRecord` with taskId, creditCode, customerNo, docIds, attIds, status
7. **Response** — return taskId + status description

Both downstream calls use `c1-token` header from config (`api.default-token`). The pipeline is wrapped in `@Transactional` — if step 4+ fails, files already uploaded to the external API cannot be rolled back (logged for manual cleanup).

### Classpath & JPA Scanning

Since JPA entities and repositories live in `shanxindai-model` (a separate JAR), the main application class uses:

```java
@SpringBootApplication
@EntityScan(basePackages = "com.example.shanxindai")
@EnableJpaRepositories(basePackages = "com.example.shanxindai")
```

All submodules share the same Java package root (`com.example.shanxindai.*`) so no import changes are needed — only the module JAR they reside in differs.

### Key Conventions

- **API path prefix:** `/techfin` (server.servlet.context-path) + `/sxd` (controller RequestMapping)
- **Response format:** `CommonResp<T>` with `code=0` for success, `code=-1` for errors
- **Jackson naming:** `SNAKE_CASE` — Java fields map to snake_case JSON
- **Error codes:** Business errors use string codes (`PARAM_MISSING`, `DUPLICATE_CREDIT_CODE`, `ATTACH_UPLOAD_FAILED`, etc.)
- **Downstream DTOs** use `@JsonIgnoreProperties(ignoreUnknown = true)` to tolerate API changes
- No tests exist yet — `src/test/` is empty

## Module Structure

```
techfin-platform/
├── pom.xml                  # Parent POM (packaging: pom)
├── shanxindai/              # 善新贷 module
│   ├── pom.xml              # Module POM
│   ├── src/                 # Source code
│   └── 文档/                # Documents
├── .gitignore
└── CLAUDE.md
```
