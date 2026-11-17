package com.ehealth;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * E-Health Care Management System
 * Educational console application. Uses Java serialization for local demo data.
 * Do not use with real patient data or in a production clinical environment.
 */
public class Main {
    private static final Scanner IN = new Scanner(System.in);
    private static final Path DATA_FILE = Paths.get("data", "ehealth.dat");
    private static Store store;
    private static User current;

    public static void main(String[] args) {
        System.out.println("\n==============================================");
        System.out.println("       E-HEALTH CARE MANAGEMENT SYSTEM");
        System.out.println("==============================================");
        try {
            store = Store.load(DATA_FILE);
            seed();
            loginLoop();
        } catch (Exception e) {
            System.out.println("Application could not start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seed() throws Exception {
        if (store.users.isEmpty()) {
            store.users.put("admin", new User("admin", hash("Admin@123"), "ADMIN", "System Administrator"));
            store.users.put("doctor", new User("doctor", hash("Doctor@123"), "DOCTOR", "Demo Doctor"));
            store.users.put("reception", new User("reception", hash("Reception@123"), "RECEPTIONIST", "Front Desk"));
            store.save(DATA_FILE);
        }
    }

    private static void loginLoop() throws Exception {
        for (;;) {
            System.out.println("\n--- Sign in ---");
            String username = ask("Username (or EXIT): ");
            if (username.equalsIgnoreCase("EXIT")) return;
            String password = ask("Password: ");
            User u = store.users.get(username.toLowerCase(Locale.ROOT));
            if (u == null || !verify(password, u.passwordHash)) {
                System.out.println("Invalid username or password.");
                continue;
            }
            current = u;
            System.out.println("\nWelcome, " + u.displayName + " [" + u.role + "]");
            mainMenu();
            current = null;
        }
    }

    private static void mainMenu() throws Exception {
        while (true) {
            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Dashboard");
            System.out.println("2. Patient management");
            System.out.println("3. Doctor management");
            System.out.println("4. Appointment management");
            System.out.println("5. Medical records");
            System.out.println("6. Prescriptions");
            System.out.println("7. Billing and payments");
            System.out.println("8. Search");
            System.out.println("9. Reports");
            System.out.println("10. User management (Admin)");
            System.out.println("11. Change my password");
            System.out.println("0. Logout");
            String c = ask("Choose: ");
            switch (c) {
                case "1" -> dashboard();
                case "2" -> patientsMenu();
                case "3" -> doctorsMenu();
                case "4" -> appointmentsMenu();
                case "5" -> recordsMenu();
                case "6" -> prescriptionsMenu();
                case "7" -> billingMenu();
                case "8" -> searchMenu();
                case "9" -> reportsMenu();
                case "10" -> usersMenu();
                case "11" -> changePassword();
                case "0" -> { store.save(DATA_FILE); return; }
                default -> System.out.println("Please choose a valid menu option.");
            }
        }
    }

    private static void dashboard() {
        long unpaid = store.bills.values().stream().filter(b -> b.balance() > 0).count();
        System.out.println("\n------------- DASHBOARD -------------");
        System.out.println("Patients       : " + store.patients.size());
        System.out.println("Doctors        : " + store.doctors.size());
        System.out.println("Appointments   : " + store.appointments.size());
        System.out.println("Medical records: " + store.records.size());
        System.out.println("Prescriptions  : " + store.prescriptions.size());
        System.out.println("Bills           : " + store.bills.size());
        System.out.println("Bills with due  : " + unpaid);
        System.out.println("Today's visits  : " + store.appointments.values().stream()
                .filter(a -> a.date.equals(LocalDate.now()) && !a.status.equals("CANCELLED")).count());
    }

    private static void patientsMenu() throws Exception {
        while (true) {
            System.out.println("\n--- PATIENTS ---\n1 Add  2 List  3 View  4 Update  5 Deactivate  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> addPatient();
                case "2" -> store.patients.values().forEach(Main::printPatient);
                case "3" -> { Patient p = findPatient(); if (p != null) printPatient(p); }
                case "4" -> updatePatient();
                case "5" -> { Patient p = findPatient(); if (p != null) { p.active = false; save(); System.out.println("Patient deactivated."); } }
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void addPatient() throws Exception {
        String name = required("Full name: ");
        int age = number("Age: ", 0, 125);
        String gender = required("Gender: ");
        String phone = required("Phone: ");
        String email = ask("Email: ");
        String blood = ask("Blood group: ");
        String address = ask("Address: ");
        String emergency = ask("Emergency contact: ");
        String id = store.id("PAT");
        Patient p = new Patient(id, name, age, gender, phone, email, blood, address, emergency);
        store.patients.put(id, p); save();
        System.out.println("Patient created. ID: " + id);
    }

    private static void updatePatient() throws Exception {
        Patient p = findPatient(); if (p == null) return;
        p.name = optional("Name [" + p.name + "]: ", p.name);
        p.phone = optional("Phone [" + p.phone + "]: ", p.phone);
        p.email = optional("Email [" + p.email + "]: ", p.email);
        p.address = optional("Address [" + p.address + "]: ", p.address);
        p.bloodGroup = optional("Blood group [" + p.bloodGroup + "]: ", p.bloodGroup);
        save(); System.out.println("Patient updated.");
    }

    private static void doctorsMenu() throws Exception {
        while (true) {
            System.out.println("\n--- DOCTORS ---\n1 Add  2 List  3 View  4 Update availability  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> addDoctor();
                case "2" -> store.doctors.values().forEach(Main::printDoctor);
                case "3" -> { Doctor d = findDoctor(); if (d != null) printDoctor(d); }
                case "4" -> { Doctor d = findDoctor(); if (d != null) { d.available = !d.available; save(); System.out.println("Availability: " + d.available); } }
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void addDoctor() throws Exception {
        String name = required("Doctor name: ");
        String specialization = required("Specialization: ");
        String phone = required("Phone: ");
        String email = ask("Email: ");
        double fee = decimal("Consultation fee: ", 0, 1000000);
        String id = store.id("DOC");
        store.doctors.put(id, new Doctor(id, name, specialization, phone, email, fee));
        save(); System.out.println("Doctor created. ID: " + id);
    }

    private static void appointmentsMenu() throws Exception {
        while (true) {
            System.out.println("\n--- APPOINTMENTS ---\n1 Book  2 List  3 Update status  4 Reschedule  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> bookAppointment();
                case "2" -> store.appointments.values().forEach(Main::printAppointment);
                case "3" -> updateAppointmentStatus();
                case "4" -> rescheduleAppointment();
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void bookAppointment() throws Exception {
        Patient p = findPatient(); if (p == null || !p.active) { System.out.println("Active patient required."); return; }
        Doctor d = findDoctor(); if (d == null || !d.available) { System.out.println("Available doctor required."); return; }
        LocalDate date = date("Appointment date (YYYY-MM-DD): ");
        LocalTime time = time("Appointment time (HH:MM): ");
        boolean clash = store.appointments.values().stream().anyMatch(a ->
                a.doctorId.equals(d.id) && a.date.equals(date) && a.time.equals(time) &&
                        !a.status.equals("CANCELLED"));
        if (clash) { System.out.println("That doctor already has an appointment at this time."); return; }
        String reason = required("Reason for visit: ");
        String id = store.id("APT");
        Appointment a = new Appointment(id, p.id, d.id, date, time, reason);
        store.appointments.put(id, a); save();
        System.out.println("Appointment booked. ID: " + id);
    }

    private static void updateAppointmentStatus() throws Exception {
        Appointment a = findAppointment(); if (a == null) return;
        System.out.println("Statuses: SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW");
        String s = required("New status: ").toUpperCase(Locale.ROOT);
        if (!Set.of("SCHEDULED","CONFIRMED","COMPLETED","CANCELLED","NO_SHOW").contains(s)) {
            System.out.println("Invalid status."); return;
        }
        a.status = s; save(); System.out.println("Status updated.");
    }

    private static void rescheduleAppointment() throws Exception {
        Appointment a = findAppointment(); if (a == null) return;
        LocalDate date = date("New date (YYYY-MM-DD): ");
        LocalTime time = time("New time (HH:MM): ");
        boolean clash = store.appointments.values().stream().anyMatch(x -> !x.id.equals(a.id) &&
                x.doctorId.equals(a.doctorId) && x.date.equals(date) && x.time.equals(time) &&
                !x.status.equals("CANCELLED"));
        if (clash) { System.out.println("Doctor is already booked at that time."); return; }
        a.date = date; a.time = time; save(); System.out.println("Appointment rescheduled.");
    }

    private static void recordsMenu() throws Exception {
        while (true) {
            System.out.println("\n--- MEDICAL RECORDS ---\n1 Add record  2 List  3 View patient history  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> addRecord();
                case "2" -> store.records.values().forEach(Main::printRecord);
                case "3" -> {
                    Patient p = findPatient();
                    if (p != null) store.records.values().stream().filter(r -> r.patientId.equals(p.id))
                            .forEach(Main::printRecord);
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void addRecord() throws Exception {
        Patient p = findPatient(); if (p == null) return;
        Doctor d = findDoctor(); if (d == null) return;
        LocalDate date = date("Visit date (YYYY-MM-DD): ");
        String symptoms = required("Symptoms: ");
        String diagnosis = required("Diagnosis: ");
        String treatment = required("Treatment/notes: ");
        String notes = ask("Additional notes: ");
        String id = store.id("REC");
        store.records.put(id, new MedicalRecord(id, p.id, d.id, date, symptoms, diagnosis, treatment, notes));
        save(); System.out.println("Medical record saved. ID: " + id);
    }

    private static void prescriptionsMenu() throws Exception {
        while (true) {
            System.out.println("\n--- PRESCRIPTIONS ---\n1 Create  2 List  3 Patient prescriptions  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> addPrescription();
                case "2" -> store.prescriptions.values().forEach(Main::printPrescription);
                case "3" -> {
                    Patient p = findPatient();
                    if (p != null) store.prescriptions.values().stream().filter(x -> x.patientId.equals(p.id))
                            .forEach(Main::printPrescription);
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void addPrescription() throws Exception {
        Patient p = findPatient(); if (p == null) return;
        Doctor d = findDoctor(); if (d == null) return;
        LocalDate date = date("Prescription date (YYYY-MM-DD): ");
        int count = number("Number of medicines: ", 1, 30);
        List<String> meds = new ArrayList<>();
        for (int i=1; i<=count; i++) meds.add(required("Medicine " + i + " (name, dose, frequency, duration): "));
        String advice = ask("Advice: ");
        String id = store.id("PRE");
        store.prescriptions.put(id, new Prescription(id, p.id, d.id, date, meds, advice));
        save(); System.out.println("Prescription saved. ID: " + id);
    }

    private static void billingMenu() throws Exception {
        while (true) {
            System.out.println("\n--- BILLING & PAYMENTS ---\n1 Create bill  2 List bills  3 Record payment  4 Patient account  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> createBill();
                case "2" -> store.bills.values().forEach(Main::printBill);
                case "3" -> recordPayment();
                case "4" -> {
                    Patient p = findPatient();
                    if (p != null) store.bills.values().stream().filter(b -> b.patientId.equals(p.id))
                            .forEach(Main::printBill);
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void createBill() throws Exception {
        Patient p = findPatient(); if (p == null) return;
        String description = required("Description (consultation/test/medicine): ");
        double amount = decimal("Total amount: ", 0.01, 100000000);
        String id = store.id("BILL");
        store.bills.put(id, new Bill(id, p.id, description, amount));
        save(); System.out.println("Bill created. ID: " + id);
    }

    private static void recordPayment() throws Exception {
        Bill b = findBill(); if (b == null) return;
        System.out.printf(Locale.ROOT, "Outstanding balance: %.2f%n", b.balance());
        if (b.balance() <= 0) { System.out.println("Bill is already paid."); return; }
        double amount = decimal("Payment amount: ", 0.01, b.balance());
        String method = required("Method (Cash/UPI/Card/Bank): ");
        String reference = ask("Transaction/reference number: ");
        b.payments.add(new Payment(LocalDate.now(), amount, method, reference));
        save(); System.out.println("Payment recorded. Remaining: " + String.format(Locale.ROOT, "%.2f", b.balance()));
    }

    private static void searchMenu() {
        String q = required("Search patient by ID/name/phone: ").toLowerCase(Locale.ROOT);
        store.patients.values().stream().filter(p -> p.id.toLowerCase().contains(q) ||
                p.name.toLowerCase().contains(q) || p.phone.toLowerCase().contains(q)).forEach(Main::printPatient);
    }

    private static void reportsMenu() {
        System.out.println("\n--- REPORTS ---");
        double billed = store.bills.values().stream().mapToDouble(b -> b.total).sum();
        double received = store.bills.values().stream().flatMap(b -> b.payments.stream()).mapToDouble(p -> p.amount).sum();
        System.out.printf(Locale.ROOT, "Total billed       : %.2f%n", billed);
        System.out.printf(Locale.ROOT, "Total collected    : %.2f%n", received);
        System.out.printf(Locale.ROOT, "Outstanding amount : %.2f%n", billed - received);
        System.out.println("\nAppointment status summary:");
        for (String status : List.of("SCHEDULED","CONFIRMED","COMPLETED","CANCELLED","NO_SHOW"))
            System.out.println(status + ": " + store.appointments.values().stream().filter(a -> a.status.equals(status)).count());
        System.out.println("\nAppointments by date:");
        store.appointments.values().stream().sorted(Comparator.comparing(a -> a.date)).forEach(Main::printAppointment);
    }

    private static void usersMenu() throws Exception {
        if (!current.role.equals("ADMIN")) { System.out.println("Access denied. Admin only."); return; }
        while (true) {
            System.out.println("\n--- USER MANAGEMENT ---\n1 Create user  2 List users  0 Back");
            switch (ask("Choose: ")) {
                case "1" -> {
                    String username = required("Username: ").toLowerCase(Locale.ROOT);
                    if (store.users.containsKey(username)) { System.out.println("Username already exists."); break; }
                    String display = required("Display name: ");
                    String role = required("Role (ADMIN/DOCTOR/RECEPTIONIST): ").toUpperCase(Locale.ROOT);
                    if (!Set.of("ADMIN","DOCTOR","RECEPTIONIST").contains(role)) { System.out.println("Invalid role."); break; }
                    String password = required("Temporary password (min 8 chars): ");
                    if (password.length() < 8) { System.out.println("Password too short."); break; }
                    store.users.put(username, new User(username, hash(password), role, display));
                    save(); System.out.println("User created.");
                }
                case "2" -> store.users.values().forEach(u -> System.out.println(u.username + " | " + u.displayName + " | " + u.role));
                case "0" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void changePassword() throws Exception {
        String old = required("Current password: ");
        if (!verify(old, current.passwordHash)) { System.out.println("Incorrect password."); return; }
        String next = required("New password (min 8 chars): ");
        if (next.length() < 8) { System.out.println("Password must be at least 8 characters."); return; }
        current.passwordHash = hash(next); save(); System.out.println("Password changed.");
    }

    private static Patient findPatient() {
        String id = required("Patient ID: ").toUpperCase(Locale.ROOT);
        Patient p = store.patients.get(id);
        if (p == null) System.out.println("Patient not found.");
        return p;
    }
    private static Doctor findDoctor() {
        String id = required("Doctor ID: ").toUpperCase(Locale.ROOT);
        Doctor d = store.doctors.get(id);
        if (d == null) System.out.println("Doctor not found.");
        return d;
    }
    private static Appointment findAppointment() {
        String id = required("Appointment ID: ").toUpperCase(Locale.ROOT);
        Appointment a = store.appointments.get(id);
        if (a == null) System.out.println("Appointment not found.");
        return a;
    }
    private static Bill findBill() {
        String id = required("Bill ID: ").toUpperCase(Locale.ROOT);
        Bill b = store.bills.get(id);
        if (b == null) System.out.println("Bill not found.");
        return b;
    }

    private static void printPatient(Patient p) {
        System.out.printf("%s | %s | Age %d | %s | %s | %s | Active=%s%n",
                p.id,p.name,p.age,p.gender,p.phone,p.bloodGroup,p.active);
    }
    private static void printDoctor(Doctor d) {
        System.out.printf(Locale.ROOT,"%s | Dr. %s | %s | %s | Fee %.2f | Available=%s%n",
                d.id,d.name,d.specialization,d.phone,d.fee,d.available);
    }
    private static void printAppointment(Appointment a) {
        System.out.printf("%s | Patient %s | Doctor %s | %s %s | %s | %s%n",
                a.id,a.patientId,a.doctorId,a.date,a.time,a.status,a.reason);
    }
    private static void printRecord(MedicalRecord r) {
        System.out.printf("%s | %s | %s | %s | Diagnosis: %s | Treatment: %s%n",
                r.id,r.patientId,r.doctorId,r.date,r.diagnosis,r.treatment);
    }
    private static void printPrescription(Prescription p) {
        System.out.println(p.id+" | "+p.patientId+" | "+p.doctorId+" | "+p.date+" | "+String.join("; ",p.medicines)+" | Advice: "+p.advice);
    }
    private static void printBill(Bill b) {
        System.out.printf(Locale.ROOT,"%s | %s | %s | Total %.2f | Paid %.2f | Due %.2f%n",
                b.id,b.patientId,b.description,b.total,b.paid(),b.balance());
    }

    private static String ask(String prompt) { System.out.print(prompt); return IN.nextLine().trim(); }
    private static String required(String prompt) {
        while (true) { String s=ask(prompt); if (!s.isBlank()) return s; System.out.println("This field is required."); }
    }
    private static String optional(String prompt, String old) { String s=ask(prompt); return s.isBlank()?old:s; }
    private static int number(String prompt, int min, int max) {
        while (true) try { int n=Integer.parseInt(required(prompt)); if(n>=min&&n<=max)return n; } catch(NumberFormatException ignored) {}
        // unreachable
    }
    private static double decimal(String prompt, double min, double max) {
        while (true) try { double n=Double.parseDouble(required(prompt)); if(Double.isFinite(n)&&n>=min&&n<=max)return n; } catch(NumberFormatException ignored) {}
    }
    private static LocalDate date(String prompt) {
        while(true) try { return LocalDate.parse(required(prompt)); } catch(DateTimeParseException e) { System.out.println("Use YYYY-MM-DD."); }
    }
    private static LocalTime time(String prompt) {
        while(true) try { return LocalTime.parse(required(prompt)); } catch(DateTimeParseException e) { System.out.println("Use HH:MM (24-hour)."); }
    }
    private static void save() throws IOException { store.save(DATA_FILE); }

    private static String hash(String password) throws Exception {
        byte[] salt = new byte[16]; SecureRandom.getInstanceStrong().nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(key);
    }
    private static boolean verify(String password, String encoded) throws Exception {
        String[] parts=encoded.split(":"); byte[] salt=Base64.getDecoder().decode(parts[0]);
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,120000,256);
        byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return MessageDigest.isEqual(key,Base64.getDecoder().decode(parts[1]));
    }

    static class Store implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        Map<String,User> users=new LinkedHashMap<>();
        Map<String,Patient> patients=new LinkedHashMap<>();
        Map<String,Doctor> doctors=new LinkedHashMap<>();
        Map<String,Appointment> appointments=new LinkedHashMap<>();
        Map<String,MedicalRecord> records=new LinkedHashMap<>();
        Map<String,Prescription> prescriptions=new LinkedHashMap<>();
        Map<String,Bill> bills=new LinkedHashMap<>();
        private int sequence=1000;
        String id(String prefix) { return prefix+"-"+(++sequence); }
        static Store load(Path path) throws IOException {
            if(!Files.exists(path)) return new Store();
            try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(path))) {
                Object o=in.readObject(); if(o instanceof Store s)return s; return new Store();
            } catch(ClassNotFoundException | InvalidClassException e) { throw new IOException("Data file is incompatible. Back it up and start with a fresh data file.",e); }
        }
        void save(Path path) throws IOException {
            Files.createDirectories(path.getParent());
            Path temp=path.resolveSibling(path.getFileName()+".tmp");
            try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(temp))) { out.writeObject(this); }
            try { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        }
    }
    static class User implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String username,passwordHash,role,displayName;
        User(String u,String p,String r,String d){username=u;passwordHash=p;role=r;displayName=d;}
    }
    static class Patient implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String id,name,gender,phone,email,bloodGroup,address,emergencyContact; int age; boolean active=true;
        Patient(String id,String n,int a,String g,String ph,String em,String bg,String ad,String ec){
            this.id=id;name=n;age=a;gender=g;phone=ph;email=em;bloodGroup=bg;address=ad;emergencyContact=ec;
        }
    }
    static class Doctor implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String id,name,specialization,phone,email; double fee; boolean available=true;
        Doctor(String id,String n,String s,String p,String e,double f){this.id=id;name=n;specialization=s;phone=p;email=e;fee=f;}
    }
    static class Appointment implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String id,patientId,doctorId,reason,status="SCHEDULED"; LocalDate date; LocalTime time;
        Appointment(String i,String p,String d,LocalDate dt,LocalTime tm,String r){id=i;patientId=p;doctorId=d;date=dt;time=tm;reason=r;}
    }
    static class MedicalRecord implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String id,patientId,doctorId,symptoms,diagnosis,treatment,notes; LocalDate date;
        MedicalRecord(String i,String p,String d,LocalDate dt,String s,String dg,String t,String n){id=i;patientId=p;doctorId=d;date=dt;symptoms=s;diagnosis=dg;treatment=t;notes=n;}
    }
    static class Prescription implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String id,patientId,doctorId,advice; LocalDate date; List<String> medicines;
        Prescription(String i,String p,String d,LocalDate dt,List<String> m,String a){id=i;patientId=p;doctorId=d;date=dt;medicines=new ArrayList<>(m);advice=a;}
    }
    static class Payment implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        LocalDate date; double amount; String method,reference;
        Payment(LocalDate d,double a,String m,String r){date=d;amount=a;method=m;reference=r;}
    }
    static class Bill implements Serializable {
        @Serial private static final long serialVersionUID=1L;
        String id,patientId,description; double total; List<Payment> payments=new ArrayList<>();
        Bill(String i,String p,String d,double t){id=i;patientId=p;description=d;total=t;}
        double paid(){return payments.stream().mapToDouble(p->p.amount).sum();}
        double balance(){return Math.max(0,total-paid());}
    }
}
