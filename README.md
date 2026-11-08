# 🚀 Enterprise AI Code Review Assistant

> A production-grade, full-stack automated code review platform integrating **Google Gemini AI**, **human-in-the-loop validation workflows**, and robust backend resilience.

[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg?style=flat-square)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-Vite-blue.svg?style=flat-square)](https://react.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

---

## 💡 Executive Summary
The **AI Code Review Assistant** is designed to bridge the gap between AI-driven developer productivity and rigorous engineering governance. Rather than blindly applying automated suggestions, this platform implements a **Human-in-the-Loop (HITL)** architecture where every security flaw, logic bug, and readability enhancement must be explicitly reviewed, accepted, or rejected by a human engineer. 

Built with enterprise scalability in mind, the system features automated fallback resilience, clean REST contracts, and comprehensive state persistence.

---

## 🛠️ Key Technical Highlights

* **🤖 Advanced AI Integration**: Interfaces with Google Gemini Flash models to analyze raw source code and unified Git diffs for security vulnerabilities, runtime exceptions, and antipatterns.
* **🛡️ Human-in-the-Loop Persistence**: Enforces explicit developer sign-off. Decisions (`ACCEPTED` / `REJECTED`) are tracked in real time and persisted via **Spring Data JPA** and an **H2 in-memory database**.
* **🔄 Fault-Tolerant Simulation Mode**: Implements intelligent fallback handling that gracefully switches to structured mock review data if API quotas or rate limits (`429`) are encountered, ensuring seamless client demonstrations.
* **📚 OpenAPI 3.0 Compliance**: Fully documented REST endpoints integrated with **Swagger UI**, allowing instant headless testing and API exploration without booting the frontend client.

---

## 🏗️ Architecture & Technology Stack

### Backend (`ai-code-review-assistant`)
* **Core Framework**: Spring Boot 3.4.1 running on **Java 17**.
* **Persistence Layer**: Spring Data JPA, Hibernate, H2 Database.
* **Serialization & Networking**: Spring MVC REST Controllers, Jackson JSR-310 (with robust unknown-property deserialization tolerance).
* **Observability**: Spring Boot Actuator health indicators and Micrometer Prometheus metrics.

### Frontend (`frontend`)
* **Framework**: React with Vite for lightning-fast bundling.
* **UI & State**: Modern dark-mode developer console featuring category filters (`BUGS`, `READABILITY`, `PERFORMANCE`), live progress tracking, and split-pane code diff review.

---

## ⚙️ Quick Start Guide

### Prerequisites
* **Java Development Kit (JDK) 17+**
* **Node.js & npm**
* A valid **Google Gemini API Key** from [Google AI Studio](https://aistudio.google.com/)

### 1. Clone & Setup Repository
```bash
git clone [https://github.com/KLUharshwardhanraj/ai-code-review-assistant.git](https://github.com/KLUharshwardhanraj/ai-code-review-assistant.git)
cd ai-code-review-assistant
