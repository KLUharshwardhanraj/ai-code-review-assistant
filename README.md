# AI Code Review Assistant 🤖📝

An enterprise-grade, full-stack application that automates code reviews and provides intelligent optimization suggestions using the Google Gemini API. Built with a robust Java/Spring Boot backend and a modern React frontend.

---

## 🚀 Tech Stack

### Backend
* **Language & Framework:** Java 17, Spring Boot 3.4.1
* **Data & Persistence:** Spring Data JPA, MySQL, H2 Database
* **Resilience & Cross-Cutting Concerns:** Spring Retry, Spring AOP
* **Observability & Documentation:** Spring Boot Actuator, Micrometer Prometheus, Springdoc OpenAPI (Swagger)
* **Build & Testing:** Maven, JaCoCo

### Frontend
* **Framework:** React.js
* **Styling / UI:** Modern CSS / Component Architecture

---

## ✨ Key Features

* **AI-Powered Code Analysis:** Integrates with the Google Gemini API to analyze code snippets, detect antipatterns, security vulnerabilities, and suggest performance optimizations.
* **Resilient Architecture:** Utilizes **Spring Retry** to handle transient API failures gracefully and **Spring AOP** for clean, centralized logging and execution metrics.
* **Production-Ready Monitoring:** Equipped with **Spring Boot Actuator** and **Micrometer Prometheus** endpoints for application health and performance tracking.
* **Interactive API Docs:** Fully documented REST endpoints via **Springdoc OpenAPI UI** (`/swagger-ui.html`).
* **Dual Database Support:** Configured with MySQL for production persistence and H2 for fast, lightweight local development and testing.

---

## 🛠️ Getting Started Locally

### Prerequisites
* Java 17 or higher
* Node.js & npm
* Maven
* Google Gemini API Key

### 1. Clone the Repository
```bash
git clone [https://github.com/KLUharshwardhanraj/ai-code-review-assistant.git](https://github.com/KLUharshwardhanraj/ai-code-review-assistant.git)
cd ai-code-review-assistant
