# Panscience Assessment

A powerful, AI-driven document and media processing web application built for the SDE-1 programming assessment. This application allows users to upload files (PDFs, Videos, Audio), automatically extracts text and transcripts, and provides a real-time, ChatGPT-like interface to ask questions about the uploaded content using hybrid semantic search.

## Tech Stack
- **Backend:** Spring Boot 3.4, Java 17, PostgreSQL, Redis, Maven
- **Frontend:** React 19, Vite, TailwindCSS (via plain CSS structure)
- **AI Integration:** OpenAI API (GPT-4o-mini, text-embedding-3-small, Whisper-1)
- **Testing:** JUnit 5, Mockito, Testcontainers, JaCoCo

## Features Implemented
- ? **Secure Authentication:** JWT-based login and registration.
- ? **Intelligent Extraction:** PDF text extraction and Whisper AI transcription for audio/video files.
- ? **Vector Search:** Chunk-based embeddings for grounded, context-aware AI answers.
- ?? **Real-Time Streaming:** Server-Sent Events (SSE) for a responsive, typing-effect chatbot.
- ? **API Documentation:** Interactive Swagger UI.
- ? **Smart Fallback:** Local document search fallback if AI quota is reached.
- ? **High Test Coverage:** Strictly enforced 95%+ line coverage.

---

## How to Run Locally

### Prerequisites
- Java 17+ and Maven
- Node.js 22+
- PostgreSQL (running locally or via Docker)
- An active OpenAI API Key

### 1. Database Setup
Create a PostgreSQL database named `panscience` (or match your local credentials in `backend/src/main/resources/application.yml`).

### 2. Start the Backend
Open a terminal in the `backend` directory:
```bash
export OPENAI_API_KEY="your-sk-api-key"
mvn spring-boot:run
```
The backend will start on `http://localhost:8080`.

### 3. Start the Frontend
Open another terminal in the `frontend` directory:
```bash
npm install
npm run dev
```
The application will be live at `http://localhost:5173`.

---

## Interactive API Documentation (Swagger)
Once the backend is running, you can explore and test all REST endpoints directly in your browser!
Navigate to: **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

> **Note:** To test secured endpoints in Swagger, first register/login via the `/api/auth` endpoints, copy the `token` from the response, and click the **Authorize** button at the top of the Swagger page.

---

## Running the Tests & Coverage Report
This project maintains a strict 95% minimum code coverage threshold. 
To run the tests and generate the JaCoCo coverage report:
```bash
cd backend
mvn clean test jacoco:report
```
*If coverage drops below 95%, the build will fail automatically.* You can view the detailed HTML report at `backend/target/site/jacoco/index.html`.
