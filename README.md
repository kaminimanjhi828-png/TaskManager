# SmartTask AI — Personal Task Manager

A smart and user-friendly desktop application built using **Java Swing and MySQL** to help users manage tasks, track progress, organize deadlines, and plan their daily activities.

SmartTask AI features a modern dashboard, task management tools, and rule-based scheduling and prioritization to help users stay organized and productive.

## Features

### Modern Dashboard

* View the total number of tasks.
* Track pending tasks.
* Monitor completed tasks.

###Task Management

* Add new tasks with a name, description, deadline, and priority.
* Update existing task details.
* Delete tasks when they are no longer needed.
* Mark tasks as completed.

### Search Tasks

* Search for tasks by name.
* View all saved tasks in a structured table.

###  Smart Daily Scheduler

* Generates a suggested daily schedule based on pending tasks.
* Allocates suggested focus time according to task priority.
* Includes breaks between scheduled tasks.

###  Smart Task Prioritizer

* Organizes pending tasks using priority and deadline information.
* Suggests which tasks should be handled first.
* Highlights overdue tasks and approaching deadlines in its recommendations.

> **Note:** The scheduling and prioritization features use local Java rules and algorithms. They do not require a paid AI API or an internet connection. The application itself still needs access to the local MySQL database.

##  Technologies Used

* **Java** — Application logic and task scheduling
* **Java Swing** — Graphical user interface
* **MySQL** — Database management
* **JDBC** — Connecting Java to MySQL
* **Visual Studio Code** — Development environment

## Prerequisites

Before running the project, make sure you have:

* Java Development Kit (JDK)
* MySQL Server
* MySQL Connector/J
* Visual Studio Code with Java support

## Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR-USERNAME/SmartTask-AI.git
```

Replace `YOUR-USERNAME` with your GitHub username.

Open the downloaded project folder in Visual Studio Code.

### 2. Create the MySQL Database

Open MySQL and execute:

```sql
CREATE DATABASE IF NOT EXISTS smarttask_db;

USE smarttask_db;

CREATE TABLE IF NOT EXISTS tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    deadline DATE NULL,
    priority VARCHAR(20) DEFAULT 'Medium',
    status VARCHAR(20) DEFAULT 'Pending'
);
```

This creates the database and table required by the application.

If your database and table already exist, you do not need to create them again.

### 3. Add MySQL Connector/J

Download MySQL Connector/J from the official website:

https://dev.mysql.com/downloads/connector/j/

Extract the downloaded archive and copy the Connector/J `.jar` file into the project's `lib` folder.

The project structure should look similar to this:

```text
SmartTask-AI/
│
├── SmartTaskAI.java
├── lib/
│   └── mysql-connector-j-<version>.jar
└── .vscode/
    └── settings.json
```

In `.vscode/settings.json`, add:

```json
{
    "java.project.referencedLibraries": [
        "lib/**/*.jar"
    ]
}
```

### 4. Configure the MySQL Password

For security, the database password is read from an environment variable instead of being written directly in the Java source code.

On Windows, open PowerShell and execute:

```powershell
setx DB_PASSWORD "YOUR_MYSQL_PASSWORD"
```

Replace `YOUR_MYSQL_PASSWORD` with your own MySQL password.

Restart Visual Studio Code after setting the variable.

**Security reminder:** Never upload your database password, API keys, or other secrets to GitHub.

### 5. Run the Application

1. Open `SmartTaskAI.java` in Visual Studio Code.
2. Make sure MySQL Server is running.
3. Confirm that the MySQL Connector/J library is configured.
4. Confirm that the `DB_PASSWORD` environment variable is set.
5. Click **Run Java**.

The SmartTask AI desktop application should open if the configuration is correct.

## 📸 Application Overview

The application includes a modern dashboard, task entry form, searchable task table, task statistics, and smart scheduling and prioritization tools.

You can add screenshots of your running application here to showcase its interface.

Example:

```markdown
![SmartTask AI Dashboard](screenshots/dashboard.png)
```

To display this image on GitHub, add a screenshot named `dashboard.png` inside a `screenshots` folder in your repository.

## Project Objective

The goal of SmartTask AI is to make everyday task management easier by combining a graphical interface, database storage, and simple rule-based scheduling and prioritization.

This project demonstrates practical implementation of Java programming, GUI development, JDBC connectivity, MySQL database operations, and basic scheduling algorithms.

##  Future Improvements

* Task filtering by priority and status
* Task completion progress bar
* Recurring tasks and reminders
* Customizable working hours
* Calendar view
* Optional integration with a local AI model

##  Author

**Kamini Manjhi**

B.Tech Computer Science and Engineering
