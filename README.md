# 📧 Smart Email Assistant  

[![Java](https://img.shields.io/badge/Java-24-red?logo=java)](https://www.oracle.com/java/)  
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)  
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://reactjs.org/)  
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-DB-blue?logo=postgresql)](https://www.postgresql.org/)  
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)  

An **AI-powered email assistant** that helps you **write, summarize, and customize email replies** with different tones. Built with **Spring Boot (Backend)** and **React (Frontend)**.  

---

## 🔗 Live Demo  

🌍 **Frontend App**: [Smart Email Assistant (Deployed)](https://frontend-page-adkl.onrender.com/)  

---

## ✨ Features  

- 🤖 AI-Powered Reply Generation – Smart suggestions for professional and casual emails  
- 📝 Email Summarization – Quickly condense long emails into short summaries  
- 🎭 Tone Selection – Choose between Formal, Casual, or Friendly  
- 📋 Copy-to-Clipboard – One-click copy for generated responses  
- 📱 Responsive UI – Works across desktop and mobile  

---

## 🛠 Tech Stack  

### Backend  
- Java 24  
- Spring Boot 3.5.5 (WebFlux, Security, Lombok)  
- **PostgreSQL** (Render Cloud Database)  
- JWT Authentication  
- Maven  

### Frontend  
- React 18  
- Axios for API calls  
- CSS (custom styling)  

---

## 🚀 Getting Started  

### Backend Setup  

```bash
cd Backend/email-writer-backend
mvn spring-boot:run
# Runs on http://localhost:8081
```

### Frontend Setup  

```bash
cd Frontend
npm install
npm start
# Runs on http://localhost:3000
```

---

## 🌐 Deployment  

- **Platform**: [Render](https://render.com/)  
- **Database**: PostgreSQL (Managed instance on Render)  
- **Environment Variables**:  
  - DB_URL  
  - DB_USERNAME  
  - DB_PASSWORD  
  - JWT_SECRET  
  - GEMINI_API_KEY  

---

## 📸 Screenshots  

### 🔹 Login & Registration  
![Login](./assets/screenshots/login.png)  
![Register](./assets/screenshots/register.png)  

### 🔹 Email Generator  
![Email Generator Form](./assets/screenshots/email-generator.png)  
![Generated Replies](./assets/screenshots/generated-replies.png)  

### 🔹 Saved Replies  
![Saved Replies](./assets/screenshots/saved-replies.png)  

### 🔹 Statistics Dashboard  
![Statistics](./assets/screenshots/statistics.png)  

### 🔹 Email Input  
![Email Input](./assets/screenshots/email-input.png)  

---

## 📌 Roadmap  

- ✅ JWT-based Authentication (Login/Register)  
- ✅ PostgreSQL Database Integration  
- 🔲 Multiple reply options + history  
- 🔲 Dark Mode UI  
- 🔲 Multi-language support  

---

## 🤝 Contributing  

1. Fork the repo  
2. Create a feature branch (`git checkout -b feature-name`)  
3. Commit changes (`git commit -m "Add feature"`)  
4. Push to branch (`git push origin feature-name`)  
5. Open a Pull Request  

---

## 👨‍💻 Author  

**Lakshaya Jain**  
📌 B.Tech Final Year – CSE  

---

## 📄 License  

This project is licensed under the [MIT License](LICENSE).  
