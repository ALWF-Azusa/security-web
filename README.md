# Security Web Application (security-web)

這是一個基於 **Spring Boot** 的 Web 應用程式，專門設計用於自動化解析與儲存資安掃描報告（如 OWASP ZAP 報告）。

本專案採用分層架構 (Controller, Service, Repository)，並使用 Jsoup 進行 HTML 解析，將掃描結果結構化後存入 MariaDB 資料庫。

## 🚀 功能特色 (Features)

* **報告上傳**：提供 Web 介面上傳 HTML 格式的資安掃描報告。
* **自動解析**：後端使用 `Jsoup` 自動擷取報告中的關鍵資訊。
* **資料持久化**：透過 `Spring Data JPA` 將解析後的資料存入 MariaDB。
* **RESTful API**：標準的 Spring Boot Web 架構。

## 🛠️ 技術堆疊 (Tech Stack)

* **語言**：Java 17 (相容 Java 21)
* **框架**：Spring Boot 3.2.2
* **資料庫**：MariaDB
* **ORM**：Spring Data JPA (Hibernate)
* **工具庫**：
    * `Lombok` (簡化程式碼)
    * `Jsoup` (HTML 解析)
    * `Gson` (JSON 處理)
* **建置工具**：Maven

## ⚙️ 安裝與執行 (Getting Started)

### 前置需求
* Java 17 或以上版本
* Maven 3.x
* MariaDB 資料庫

### 1. 設定資料庫
請確認你的 MariaDB 正在執行，並建立一個資料庫（例如 `security_db`）。接著在 `src/main/resources/application.properties` 中設定你的連線資訊：

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/你的資料庫名稱
spring.datasource.username=你的帳號
spring.datasource.password=你的密碼
spring.jpa.hibernate.ddl-auto=update
