# Online Code Compiler

- zh_CN [简体中文](/README.zh_CN.md)
## Introduction

Welcome to **Online Code Compiler**, an online code editing and running platform that supports **Python** and **Java** languages. This project aims to provide developers and learners with a convenient and secure online programming environment, allowing you to write, compile, and run code without installing any software locally.

## Features

- **Multi-language Support**: Currently supports Python and Java, with plans to expand to more programming languages in the future.
- **Real-time Editing and Running**: Edit code instantly and view run results to improve programming efficiency.
- **Secure Sandbox Environment**: Each code run executes in an independent Docker container, ensuring isolation and security of code execution.

## Technology Stack

- **Frontend**: React
- **Backend**: Spring Boot
- **Containerization**: Docker
- **Reverse Proxy**: Nginx
- **Deployment Platform**: AWS

## Architecture Overview

The entire project adopts a frontend-backend separated architecture, utilizing Docker containers to achieve secure code compilation and execution. The main components include:

1. **Frontend (React)**: Provides the user interface, supporting code editing and result display.
2. **Backend (Spring Boot)**: Handles user requests and manages code compilation and execution.
3. **Sandbox Environment**: Uses Docker containers to provide isolated execution environments for each user request, ensuring system security.
4. **Nginx**: Acts as a reverse proxy, managing request routing between the frontend and backend.

## Deployment Guide

The following steps will guide you on how to deploy this project in a local environment.

### Prerequisites

- **Docker**: Ensure Docker and Docker Compose are installed.
- **Git**: Used to clone the project repository.

### Running the Project

```bash
git clone https://github.com/Xinran1205/Online-Compiler.git
cd Online-Compiler
docker-compose up --build
```
The above command will start the frontend, backend, and Nginx services. Visit http://localhost to view the project.

## Accessing the Project
The project has been successfully deployed and is running on the server 35.176.123.35.