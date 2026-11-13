# Online Voting System (Simple Java Project)

## Technologies
- Java (Swing GUI)
- JDBC
- MySQL

## Features
- Voter registration
- Voter login with salted PBKDF2 password hashing
- Candidate list loaded from MySQL
- Vote casting
- Duplicate-vote prevention using a UNIQUE database constraint
- Automatic result counting

## Requirements
1. JDK 8 or newer (JDK 11+ recommended)
2. MySQL Server and MySQL Workbench
3. MySQL Connector/J JAR

## Setup
1. Open MySQL Workbench and run `database.sql`.
2. In `OnlineVotingSystem.java`, update:
   - `DB_USER`
   - `DB_PASSWORD`
   to match your MySQL credentials.
3. Download MySQL Connector/J and add its JAR to the project classpath.
4. Compile and run:

### Windows (Command Prompt)
```bat
javac -cp ".;mysql-connector-j-<version>.jar" OnlineVotingSystem.java
java -cp ".;mysql-connector-j-<version>.jar" OnlineVotingSystem
```

### macOS / Linux
```bash
javac -cp ".:mysql-connector-j-<version>.jar" OnlineVotingSystem.java
java -cp ".:mysql-connector-j-<version>.jar" OnlineVotingSystem
```

Replace `<version>` with the actual connector version/filename.

## Demo flow for presentation
1. Register two voters with different Voter IDs.
2. Log in as the first voter.
3. Select a candidate and confirm the vote.
4. Log out by closing the window and return to the home screen (restart app if needed).
5. Log in again with the same Voter ID: the system blocks a second vote.
6. Open View Results to show automatic vote totals.

## Important notes
- This is an academic demonstration project, not a certified election platform.
- Results are visible in the demo app to demonstrate counting. A real election system needs independent audits, stronger identity verification, secure infrastructure, privacy protections, and formal election procedures.
- Candidate records are inserted through `database.sql` for simplicity; no separate admin panel is included.
