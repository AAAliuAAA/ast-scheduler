# AST Scheduler

A unified scheduling management platform for AI Agents, focused on managing task scheduling for Claude Code, OpenClaw, and other AI Agents.

## Features

- **Zero Configuration** - Built-in SQLite, ready to use out of the box
- **Lightweight** - Single JAR deployment, no database installation required
- **Cross-Platform** - Supports Windows/Linux/macOS
- **Web Management** - Clean and intuitive web interface
- **Flexible Scheduling** - Supports Cron expressions, manual triggers, and task dependencies

## Quick Start

### Requirements

- JDK 17+
- Maven 3.6+ (for development)

### Run

```bash
# 1. Clone the project
git clone https://github.com/your-repo/ast-scheduler.git
cd ast-scheduler

# 2. Build
mvn clean package

# 3. Run
java -jar ast-scheduler-web/target/ast-scheduler-web-1.0.0-SNAPSHOT.jar

# 4. Access
open http://localhost:8080
```

Database file will be automatically created at `./data/scheduler.db`

### Windows Quick Start

Create `start.bat`:
```batch
@echo off
java -jar ast-scheduler-web-1.0.0-SNAPSHOT.jar
pause
```

## Tech Stack

- **Backend**: Spring Boot 3.2.3
- **Database**: SQLite (default) / MySQL (optional)
- **ORM**: MyBatis 3.0.3
- **Frontend**: Thymeleaf + Bootstrap 5

## Project Structure

```
ast-scheduler
├── ast-scheduler-common    # Common module
├── ast-scheduler-core      # Core scheduling engine
├── ast-scheduler-web       # Web management interface
└── pom.xml
```

## Configuration

### Using SQLite (Default)

No configuration needed, just run.

### Switch to MySQL

Modify `application.yml`, uncomment MySQL configuration:

```yaml
spring:
  profiles:
    active: mysql  # Change to mysql
```

Then create database:
```sql
CREATE DATABASE agent_scheduler DEFAULT CHARACTER SET utf8mb4;
```

## Modules

- **Executor Management** - Register and manage Agent executors
- **Task Management** - Create, edit, enable/disable tasks
- **Scheduling Engine** - Cron scheduling, task dependencies
- **Execution Logs** - Real-time log viewing, history records
- **Monitoring & Alerts** - Task status monitoring, failure alerts

## Development

```bash
# Start development mode
mvn spring-boot:run -pl ast-scheduler-web

# Run tests
mvn test
```

## License

Apache License 2.0

