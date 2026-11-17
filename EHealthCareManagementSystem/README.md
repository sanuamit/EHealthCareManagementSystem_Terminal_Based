# E-Health Care Management System (Core Java)

A menu-driven Java 17 console project demonstrating OOP, collections, validation, authentication, persistence, and basic healthcare-office workflows.

## Features
- Login with three roles: ADMIN, DOCTOR, RECEPTIONIST (role checks for user administration).
- Patient registration, listing, lookup, update, and deactivation.
- Doctor registration, listing, and availability toggle.
- Appointment booking, conflict checking per doctor/date/time, rescheduling, and status updates.
- Medical record entry and patient history.
- Multi-item prescriptions and patient prescription history.
- Billing, partial payments, payment methods/references, and outstanding balance.
- Patient search by ID, name, or phone.
- Revenue, outstanding balance, and appointment status reports.
- Password changes; passwords stored as PBKDF2-HMAC-SHA256 hashes.
- Automatic local persistence in `data/ehealth.dat`.

## Requirements
- JDK 17 or newer (not just a JRE).
- Optional: Maven 3.8+.

## Windows setup (recommended)
1. Extract the ZIP.
2. Install a JDK 17+ distribution (for example, Temurin or Oracle JDK).
3. Open Command Prompt or PowerShell in the extracted `EHealthCareManagementSystem` folder.
4. Check Java: `java -version` and `javac -version`.
5. Run `run.bat`.

The script compiles the source and launches the app. No third-party Java libraries are required.

## Maven setup
From the project root:
```bash
mvn clean compile
mvn exec:java
```

## Linux/macOS
```bash
chmod +x run.sh
./run.sh
```

## Demo accounts
| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Doctor | `doctor` | `Doctor@123` |
| Receptionist | `reception` | `Reception@123` |

Change demo passwords after the first login. Create additional users through **User management** while logged in as admin.

## First-use walkthrough
1. Sign in as `admin`.
2. Open **Doctor management → Add**. Example: Dr. Ananya Das, General Medicine, phone 9876543210, fee 500.
3. Open **Patient management → Add**. Example: Rahul Das, age 28, Male, phone 9876501234.
4. Note the generated IDs (e.g. `DOC-1001`, `PAT-1002`; IDs depend on creation order).
5. Open **Appointment management → Book**, enter patient and doctor IDs, then a future date and time.
6. Add a **Medical record** and a **Prescription** using the same patient/doctor IDs.
7. Create a bill, then record one or more partial payments.
8. Open **Reports** to see collection, due amount, and appointment summaries.
9. Log out. Restart the program: data remains saved.

## OOP concepts used
- **Encapsulation:** model data and operations are grouped in classes.
- **Abstraction:** menu workflows hide persistence and hashing implementation details.
- **Inheritance:** domain entities share Java's `Object` base type; this version intentionally avoids artificial inheritance between unrelated entities. You can extend it with a `Person` superclass for Patient and Doctor as an OOP exercise.
- **Polymorphism:** Java collection streams and overridden `toString` are not used as a central feature; add interfaces for repository/report strategies as an extension.

## Project structure
```text
EHealthCareManagementSystem/
├── src/main/java/com/ehealth/Main.java
├── data/                  # generated local database file
├── out/                   # compiled classes (generated)
├── pom.xml
├── run.bat
├── run.sh
├── .gitignore
└── README.md
```

## Important limitations and safety
This is a portfolio/learning project, not a certified medical product. It is not suitable for real patient records or clinical decision-making. It has no encryption-at-rest, audit trail, backups, network security, regulatory compliance, or production-grade authorization boundaries. Java native serialization is used for simple local persistence; only load data files created by this application. Do not expose this application to untrusted users or store real patient data. Prescriptions and diagnoses are manually entered examples, not medical advice.
