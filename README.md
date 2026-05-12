# Panscience Assessment

AI-powered document and multimedia Q&A web application built for the SDE-1 programming assessment.

## Target Stack

- Backend: Spring Boot
- Frontend: React + Vite + JavaScript
- Storage: PostgreSQL
- AI: OpenAI API
- Transcription: Whisper / OpenAI ASR or Deepgram
- Infra: Docker, Docker Compose, GitHub Actions

## Current Status

Phase 1 scaffold is in place:

- Spring Boot backend structure
- React frontend structure
- Docker and Docker Compose skeleton
- GitHub Actions CI skeleton
- Environment template

## Repository Layout

```text
backend/   Spring Boot application
frontend/  React application
```

## Local Prerequisites

- Java 17
- Maven 3.6.3+
- Node.js 22+
- Docker Desktop

## Planned Phases

1. Project setup and repository scaffold
2. File upload and metadata persistence
3. PDF and media processing
4. Q&A, summaries, and timestamps
5. Streaming, auth, and Redis
6. Tests, CI, and delivery polish
