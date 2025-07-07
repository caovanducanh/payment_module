# Payment Module

## Overview
A robust, production-ready payment microservice for handling payment transactions, supporting multiple gateways, observability, and security best practices.

---

## Plug & Play Integration

> **This module is designed for easy integration into any Spring Boot project.**
> Just copy the `payment` folder, update the package, and configure your environment.

### **Integration Checklist**
1. **Copy the `payment` folder** into your target project (e.g., under `src/main/java/com/yourcompany/payment`).
2. **Update all Java package declarations** to match your project's base package (e.g., `com.example.payment` → `com.yourcompany.payment`).
3. **Copy or merge the following files/folders:**
   - `src/main/resources/application.properties` (merge config as needed)
   - `pom.xml` dependencies (copy all `<dependency>` blocks to your main `pom.xml`)
4. **Configure your environment:**
   - Database (MySQL, etc.)
   - Jasypt secret (for encrypted properties)
5. **Build and run!**

### **How to Change Package**
- Use your IDE's refactor tool (recommended) or search & replace all occurrences of the old package name.
- Update `@ComponentScan` or `@SpringBootApplication(scanBasePackages=...)` if your main app is in a different package root.

### **Folder Structure**
```
payment/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/yourcompany/payment/...
│   │   └── resources/application.properties
│   └── test/...
```

---

## Features
- **Clean Domain Model**: Uses enums for status, provider, and payment method.
- **Gateway Abstraction**: Easily extendable for new payment providers.
- **Resilience & Observability**: Circuit breaker, retry, metrics, and audit logging.
- **Security**: Sensitive config encrypted with Jasypt.
- **DevOps Ready**: Docker, CI/CD, Prometheus, and clear runbook.

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Local Development
No external dependencies required. Just configure your database and run!

#### Run the Service
```bash
mvn clean spring-boot:run
```

#### Run with Docker
```bash
docker build -t payment-service .
docker run -p 8080:8080 payment-service
```

---

## Configuration

| Property | Description | Example |
|---|---|---|
| `jasypt.encryptor.password` | Jasypt encryption password | `yourSecret` |
| `management.endpoints.web.exposure.include` | Actuator endpoints | `*` |
| ... | ... | ... |

Sensitive values should be encrypted using Jasypt.

---

## Architecture

```mermaid
graph TD;
  A[Client] -->|REST: /api/payments/vnpay| B(PaymentController)
  B -->|Validate & map| C(TransactionService)
  C -->|Select gateway| D(PaymentGateway_VnPay)
  D -->|Call API| E(VNPAY_Provider)
  E -->|Redirect user| F[Client]
  E -->|Callback (return/IPN)| G(PaymentController)
  G -->|Update| C
  C -->|Save| H(TransactionRepository)
  C -->|Log| I(AuditLogger)
  I -->|Log| J(ELK_Log_Management)
  C -->|Expose| K(Actuator/Prometheus)
```

### **Flow Explanation**
1. **Client** gửi yêu cầu thanh toán (REST API) đến **PaymentController**.
2. **PaymentController** validate, map request sang domain, gọi **TransactionService**.
3. **TransactionService** chọn gateway phù hợp (ví dụ: VnPay), gọi **PaymentGateway_VnPay**.
4. **PaymentGateway_VnPay** gọi API provider thực tế (**VNPAY_Provider**).
5. **VNPAY_Provider** trả về URL, redirect user sang trang thanh toán.
6. Sau khi thanh toán, **VNPAY_Provider** gọi callback (return/IPN) về **PaymentController**.
7. **PaymentController** gọi lại **TransactionService** để update trạng thái giao dịch.
8. **TransactionService** lưu trạng thái mới vào **TransactionRepository** (DB).
9. Ghi log nghiệp vụ qua **AuditLogger** (đẩy về ELK nếu có).
10. Expose health/metrics qua **Actuator/Prometheus** để monitoring.

---

## Best Practices
- Use Spring profiles for environment-specific config.
- Encrypt secrets and sensitive config.
- Monitor with Prometheus/Actuator.
- Use circuit breaker/retry for external APIs.
- Audit all service actions.

---

## Troubleshooting
- **Sensitive config?** Use Jasypt and never commit secrets.
- **Package errors?** Double-check all package declarations and imports after refactor.

---

## Extending
- Add new payment gateways by implementing `PaymentGateway` interface.
- Add new metrics via Actuator endpoints.

---

## Runbook
- **Startup**: Ensure DB is up and required secrets are set.
- **Health**: Check `/actuator/health`.
- **Logs**: Centralized via ELK or similar.
- **Scaling**: Stateless, can be scaled horizontally.

---

## License
MIT