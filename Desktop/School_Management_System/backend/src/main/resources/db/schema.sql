-- =============================================================================
-- School Management System (SMS) - PostgreSQL Database Schema
-- =============================================================================

-- Create database
SELECT 'CREATE DATABASE sms_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sms_db')\gexec

\c sms_db;

-- =============================================================================
-- EXTENSIONS
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- DROP EXISTING TABLES (in correct order due to foreign keys)
-- =============================================================================
DROP TABLE IF EXISTS hostel_students CASCADE;
DROP TABLE IF EXISTS hostel_rooms CASCADE;
DROP TABLE IF EXISTS hostels CASCADE;
DROP TABLE IF EXISTS transport_students CASCADE;
DROP TABLE IF EXISTS transport_routes CASCADE;
DROP TABLE IF EXISTS payroll CASCADE;
DROP TABLE IF EXISTS timetables CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS borrow_records CASCADE;
DROP TABLE IF EXISTS library_books CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
DROP TABLE IF EXISTS fees CASCADE;
DROP TABLE IF EXISTS assignment_submissions CASCADE;
DROP TABLE IF EXISTS assignments CASCADE;
DROP TABLE IF EXISTS exam_results CASCADE;
DROP TABLE IF EXISTS examinations CASCADE;
DROP TABLE IF EXISTS attendance CASCADE;
DROP TABLE IF EXISTS student_classes CASCADE;
DROP TABLE IF EXISTS teacher_subjects CASCADE;
DROP TABLE IF EXISTS teachers CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS parents CASCADE;
DROP TABLE IF EXISTS subjects CASCADE;
DROP TABLE IF EXISTS streams CASCADE;
DROP TABLE IF EXISTS classes CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS terms CASCADE;
DROP TABLE IF EXISTS academic_years CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS school_info CASCADE;

-- =============================================================================
-- 1. USERS
-- =============================================================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    profile_photo VARCHAR(255),
    is_enabled BOOLEAN DEFAULT true,
    is_account_non_locked BOOLEAN DEFAULT true,
    failed_login_attempts INT DEFAULT 0,
    last_login TIMESTAMP,
    password_changed_at TIMESTAMP,
    reset_token VARCHAR(255),
    reset_token_expires TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    updated_by INT
);

-- =============================================================================
-- 2. ROLES
-- =============================================================================
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. PERMISSIONS
-- =============================================================================
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    resource VARCHAR(50),
    action VARCHAR(50)
);

-- =============================================================================
-- 4. USER_ROLES (Junction table)
-- =============================================================================
CREATE TABLE user_roles (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    UNIQUE(user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- =============================================================================
-- 5. ROLE_PERMISSIONS (Junction table)
-- =============================================================================
CREATE TABLE role_permissions (
    id SERIAL PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    UNIQUE(role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- =============================================================================
-- 6. ACADEMIC YEARS
-- =============================================================================
CREATE TABLE academic_years (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 7. TERMS
-- =============================================================================
CREATE TABLE terms (
    id SERIAL PRIMARY KEY,
    academic_year_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE
);

-- =============================================================================
-- 8. DEPARTMENTS
-- =============================================================================
CREATE TABLE departments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    head_id INT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 9. CLASSES
-- =============================================================================
CREATE TABLE classes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    section VARCHAR(20),
    department_id INT,
    academic_year_id INT,
    capacity INT DEFAULT 40,
    class_teacher_id INT,
    room_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL,
    FOREIGN KEY (class_teacher_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- 10. STREAMS
-- =============================================================================
CREATE TABLE streams (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    class_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);

-- =============================================================================
-- 11. SUBJECTS
-- =============================================================================
CREATE TABLE subjects (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    department_id INT,
    description TEXT,
    is_compulsory BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- =============================================================================
-- 12. PARENTS
-- =============================================================================
CREATE TABLE parents (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    occupation VARCHAR(100),
    relationship VARCHAR(50),
    workplace VARCHAR(150),
    annual_income DECIMAL(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================================
-- 13. STUDENTS
-- =============================================================================
CREATE TABLE students (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    admission_number VARCHAR(20) UNIQUE NOT NULL,
    admission_date DATE NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10) CHECK(gender IN ('MALE','FEMALE','OTHER')),
    nationality VARCHAR(50),
    blood_group VARCHAR(5),
    address TEXT,
    previous_school VARCHAR(150),
    medical_info TEXT,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    parent_id INT,
    stream_id INT,
    current_class_id INT,
    academic_status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE SET NULL,
    FOREIGN KEY (stream_id) REFERENCES streams(id) ON DELETE SET NULL,
    FOREIGN KEY (current_class_id) REFERENCES classes(id) ON DELETE SET NULL
);

-- =============================================================================
-- 14. TEACHERS
-- =============================================================================
CREATE TABLE teachers (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    employee_number VARCHAR(20) UNIQUE NOT NULL,
    qualification VARCHAR(100),
    department_id INT,
    specialization VARCHAR(100),
    hire_date DATE,
    employment_status VARCHAR(20) DEFAULT 'ACTIVE',
    salary DECIMAL(12,2),
    experience_years INT,
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- =============================================================================
-- 15. TEACHER_SUBJECTS
-- =============================================================================
CREATE TABLE teacher_subjects (
    id SERIAL PRIMARY KEY,
    teacher_id INT NOT NULL,
    subject_id INT NOT NULL,
    class_id INT NOT NULL,
    academic_year_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(teacher_id, subject_id, class_id, academic_year_id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE
);

-- =============================================================================
-- 16. STUDENT_CLASSES
-- =============================================================================
CREATE TABLE student_classes (
    id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    class_id INT NOT NULL,
    academic_year_id INT NOT NULL,
    enrollment_date DATE DEFAULT CURRENT_DATE,
    UNIQUE(student_id, class_id, academic_year_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE
);

-- =============================================================================
-- 17. ATTENDANCE
-- =============================================================================
CREATE TABLE attendance (
    id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    class_id INT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(10) CHECK(status IN ('PRESENT','ABSENT','SICK','LATE','EXCUSED')) NOT NULL,
    remarks TEXT,
    marked_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, class_id, date),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (marked_by) REFERENCES teachers(id) ON DELETE SET NULL
);

-- =============================================================================
-- 18. EXAMINATIONS
-- =============================================================================
CREATE TABLE examinations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) CHECK(type IN ('CAT','MIDTERM','FINAL','MOCK','NATIONAL')),
    term_id INT,
    academic_year_id INT,
    start_date DATE,
    end_date DATE,
    max_marks INT DEFAULT 100,
    pass_marks INT DEFAULT 50,
    weight DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE SET NULL,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL
);

-- =============================================================================
-- 19. EXAM_RESULTS
-- =============================================================================
CREATE TABLE exam_results (
    id SERIAL PRIMARY KEY,
    examination_id INT NOT NULL,
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    marks_obtained DECIMAL(6,2),
    grade VARCHAR(5),
    remarks TEXT,
    graded_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(examination_id, student_id, subject_id),
    FOREIGN KEY (examination_id) REFERENCES examinations(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (graded_by) REFERENCES teachers(id) ON DELETE SET NULL
);

-- =============================================================================
-- 20. ASSIGNMENTS
-- =============================================================================
CREATE TABLE assignments (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    subject_id INT,
    class_id INT,
    teacher_id INT,
    academic_year_id INT,
    due_date TIMESTAMP NOT NULL,
    max_marks INT DEFAULT 100,
    attachment_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL
);

-- =============================================================================
-- 21. ASSIGNMENT_SUBMISSIONS
-- =============================================================================
CREATE TABLE assignment_submissions (
    id SERIAL PRIMARY KEY,
    assignment_id INT NOT NULL,
    student_id INT NOT NULL,
    submission_text TEXT,
    attachment_url VARCHAR(255),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    marks DECIMAL(6,2),
    grade VARCHAR(5),
    feedback TEXT,
    graded_by INT,
    status VARCHAR(20) DEFAULT 'SUBMITTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (graded_by) REFERENCES teachers(id) ON DELETE SET NULL
);

-- =============================================================================
-- 22. FEES
-- =============================================================================
CREATE TABLE fees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    amount DECIMAL(12,2) NOT NULL,
    fee_type VARCHAR(50),
    class_id INT,
    academic_year_id INT,
    term_id INT,
    is_mandatory BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL,
    FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE SET NULL
);

-- =============================================================================
-- 23. INVOICES
-- =============================================================================
CREATE TABLE invoices (
    id SERIAL PRIMARY KEY,
    invoice_number VARCHAR(30) UNIQUE NOT NULL,
    student_id INT NOT NULL,
    fee_id INT,
    total_amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) DEFAULT 0,
    discount DECIMAL(12,2) DEFAULT 0,
    due_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (fee_id) REFERENCES fees(id) ON DELETE SET NULL
);

-- =============================================================================
-- 24. PAYMENTS
-- =============================================================================
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    payment_number VARCHAR(30) UNIQUE NOT NULL,
    invoice_id INT,
    student_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(30),
    transaction_reference VARCHAR(100),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    received_by INT,
    receipt_number VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (received_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- 25. LIBRARY_BOOKS
-- =============================================================================
CREATE TABLE library_books (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150),
    isbn VARCHAR(20) UNIQUE,
    publisher VARCHAR(150),
    category VARCHAR(50),
    edition VARCHAR(30),
    quantity INT DEFAULT 1,
    available_quantity INT DEFAULT 1,
    location VARCHAR(50),
    barcode VARCHAR(50) UNIQUE,
    year_published INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 26. BORROW_RECORDS
-- =============================================================================
CREATE TABLE borrow_records (
    id SERIAL PRIMARY KEY,
    book_id INT NOT NULL,
    borrower_id INT NOT NULL,
    borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP,
    status VARCHAR(20) DEFAULT 'BORROWED',
    fine_amount DECIMAL(8,2) DEFAULT 0,
    issued_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES library_books(id) ON DELETE CASCADE,
    FOREIGN KEY (borrower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- 27. INVENTORY
-- =============================================================================
CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    quantity INT DEFAULT 0,
    unit_price DECIMAL(10,2),
    supplier VARCHAR(150),
    purchase_date DATE,
    location VARCHAR(100),
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 28. EVENTS
-- =============================================================================
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(50),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    location VARCHAR(150),
    organized_by INT,
    is_public BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organized_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- 29. NOTIFICATIONS
-- =============================================================================
CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30),
    sender_id INT,
    priority VARCHAR(10) DEFAULT 'NORMAL',
    is_read BOOLEAN DEFAULT false,
    target_role VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- 30. AUDIT_LOGS
-- =============================================================================
CREATE TABLE audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INT,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id INT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- 31. TIMETABLES
-- =============================================================================
CREATE TABLE timetables (
    id SERIAL PRIMARY KEY,
    class_id INT NOT NULL,
    subject_id INT,
    teacher_id INT,
    day_of_week VARCHAR(10),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(20),
    academic_year_id INT,
    term_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL,
    FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE SET NULL
);

-- =============================================================================
-- 32. PAYROLL
-- =============================================================================
CREATE TABLE payroll (
    id SERIAL PRIMARY KEY,
    teacher_id INT NOT NULL,
    basic_salary DECIMAL(12,2),
    allowances DECIMAL(12,2) DEFAULT 0,
    deductions DECIMAL(12,2) DEFAULT 0,
    tax DECIMAL(12,2) DEFAULT 0,
    net_salary DECIMAL(12,2),
    payment_date DATE,
    month INT,
    year INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);

-- =============================================================================
-- 33. TRANSPORT_ROUTES
-- =============================================================================
CREATE TABLE transport_routes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    bus_number VARCHAR(20),
    driver_name VARCHAR(100),
    driver_phone VARCHAR(20),
    route_description TEXT,
    capacity INT,
    fare DECIMAL(8,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 34. TRANSPORT_STUDENTS
-- =============================================================================
CREATE TABLE transport_students (
    id SERIAL PRIMARY KEY,
    transport_route_id INT NOT NULL,
    student_id INT NOT NULL,
    pickup_point VARCHAR(100),
    UNIQUE(transport_route_id, student_id),
    FOREIGN KEY (transport_route_id) REFERENCES transport_routes(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- =============================================================================
-- 35. HOSTELS
-- =============================================================================
CREATE TABLE hostels (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    capacity INT,
    warden_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warden_id) REFERENCES teachers(id) ON DELETE SET NULL
);

-- =============================================================================
-- 36. HOSTEL_ROOMS
-- =============================================================================
CREATE TABLE hostel_rooms (
    id SERIAL PRIMARY KEY,
    hostel_id INT NOT NULL,
    room_number VARCHAR(20),
    capacity INT DEFAULT 2,
    occupied INT DEFAULT 0,
    room_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hostel_id) REFERENCES hostels(id) ON DELETE CASCADE
);

-- =============================================================================
-- 37. HOSTEL_STUDENTS
-- =============================================================================
CREATE TABLE hostel_students (
    id SERIAL PRIMARY KEY,
    hostel_room_id INT NOT NULL,
    student_id INT NOT NULL,
    check_in_date DATE,
    check_out_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    UNIQUE(hostel_room_id, student_id),
    FOREIGN KEY (hostel_room_id) REFERENCES hostel_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- =============================================================================
-- 38. SCHOOL_INFO
-- =============================================================================
CREATE TABLE school_info (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(150),
    logo VARCHAR(255),
    motto VARCHAR(200),
    established_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- INDEXES
-- =============================================================================

-- Users
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_last_login ON users(last_login);

-- Students
CREATE INDEX idx_students_admission_number ON students(admission_number);
CREATE INDEX idx_students_parent_id ON students(parent_id);
CREATE INDEX idx_students_current_class_id ON students(current_class_id);
CREATE INDEX idx_students_academic_status ON students(academic_status);
CREATE INDEX idx_students_stream_id ON students(stream_id);

-- Teachers
CREATE INDEX idx_teachers_employee_number ON teachers(employee_number);
CREATE INDEX idx_teachers_department_id ON teachers(department_id);
CREATE INDEX idx_teachers_employment_status ON teachers(employment_status);

-- Attendance
CREATE INDEX idx_attendance_student_id ON attendance(student_id);
CREATE INDEX idx_attendance_date ON attendance(date);
CREATE INDEX idx_attendance_class_id ON attendance(class_id);

-- Exam Results
CREATE INDEX idx_exam_results_examination_id ON exam_results(examination_id);
CREATE INDEX idx_exam_results_student_id ON exam_results(student_id);
CREATE INDEX idx_exam_results_subject_id ON exam_results(subject_id);

-- Payments
CREATE INDEX idx_payments_student_id ON payments(student_id);
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);

-- Borrow Records
CREATE INDEX idx_borrow_records_borrower_id ON borrow_records(borrower_id);
CREATE INDEX idx_borrow_records_book_id ON borrow_records(book_id);
CREATE INDEX idx_borrow_records_status ON borrow_records(status);

-- Audit Logs
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);

-- Timetables
CREATE INDEX idx_timetables_class_id ON timetables(class_id);
CREATE INDEX idx_timetables_day_of_week ON timetables(day_of_week);
CREATE INDEX idx_timetables_teacher_id ON timetables(teacher_id);

-- Notifications
CREATE INDEX idx_notifications_target_role ON notifications(target_role);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_sender_id ON notifications(sender_id);

-- Invoices
CREATE INDEX idx_invoices_student_id ON invoices(student_id);
CREATE INDEX idx_invoices_status ON invoices(status);

-- Assignments
CREATE INDEX idx_assignments_class_id ON assignments(class_id);
CREATE INDEX idx_assignments_subject_id ON assignments(subject_id);
CREATE INDEX idx_assignments_due_date ON assignments(due_date);

-- Assignment Submissions
CREATE INDEX idx_assignment_submissions_assignment_id ON assignment_submissions(assignment_id);
CREATE INDEX idx_assignment_submissions_student_id ON assignment_submissions(student_id);
CREATE INDEX idx_assignment_submissions_status ON assignment_submissions(status);

-- Events
CREATE INDEX idx_events_event_type ON events(event_type);
CREATE INDEX idx_events_start_date ON events(start_date);

-- Inventory
CREATE INDEX idx_inventory_category ON inventory(category);
CREATE INDEX idx_inventory_status ON inventory(status);

-- Payroll
CREATE INDEX idx_payroll_teacher_id ON payroll(teacher_id);
CREATE INDEX idx_payroll_status ON payroll(status);
CREATE INDEX idx_payroll_payment_date ON payroll(payment_date);

-- =============================================================================
-- INITIAL DATA: ROLES
-- =============================================================================
INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Super Administrator with full system access'),
    ('SCHOOL_ADMIN', 'School Administrator with administrative access'),
    ('TEACHER', 'Teacher with academic management access'),
    ('STUDENT', 'Student with limited access'),
    ('PARENT', 'Parent with view access to child information'),
    ('ACCOUNTANT', 'Accountant with financial management access'),
    ('LIBRARIAN', 'Librarian with library management access'),
    ('REGISTRAR', 'Registrar with student records management access'),
    ('RECEPTIONIST', 'Receptionist with front desk access'),
    ('INVENTORY_MANAGER', 'Inventory manager with stock control access'),
    ('TRANSPORT_MANAGER', 'Transport manager with route and vehicle access'),
    ('HOSTEL_MANAGER', 'Hostel manager with accommodation access');

-- =============================================================================
-- INITIAL DATA: PERMISSIONS
-- =============================================================================

-- Student permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('STUDENT_CREATE', 'Create new students', 'STUDENT', 'CREATE'),
    ('STUDENT_READ', 'View student information', 'STUDENT', 'READ'),
    ('STUDENT_UPDATE', 'Update student information', 'STUDENT', 'UPDATE'),
    ('STUDENT_DELETE', 'Delete student records', 'STUDENT', 'DELETE'),
    ('STUDENT_LIST', 'List all students', 'STUDENT', 'LIST'),
    ('STUDENT_EXPORT', 'Export student data', 'STUDENT', 'EXPORT');

-- Teacher permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('TEACHER_CREATE', 'Create new teachers', 'TEACHER', 'CREATE'),
    ('TEACHER_READ', 'View teacher information', 'TEACHER', 'READ'),
    ('TEACHER_UPDATE', 'Update teacher information', 'TEACHER', 'UPDATE'),
    ('TEACHER_DELETE', 'Delete teacher records', 'TEACHER', 'DELETE'),
    ('TEACHER_LIST', 'List all teachers', 'TEACHER', 'LIST'),
    ('TEACHER_EXPORT', 'Export teacher data', 'TEACHER', 'EXPORT');

-- Class permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('CLASS_CREATE', 'Create new classes', 'CLASS', 'CREATE'),
    ('CLASS_READ', 'View class information', 'CLASS', 'READ'),
    ('CLASS_UPDATE', 'Update class information', 'CLASS', 'UPDATE'),
    ('CLASS_DELETE', 'Delete class records', 'CLASS', 'DELETE'),
    ('CLASS_LIST', 'List all classes', 'CLASS', 'LIST');

-- Subject permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('SUBJECT_CREATE', 'Create new subjects', 'SUBJECT', 'CREATE'),
    ('SUBJECT_READ', 'View subject information', 'SUBJECT', 'READ'),
    ('SUBJECT_UPDATE', 'Update subject information', 'SUBJECT', 'UPDATE'),
    ('SUBJECT_DELETE', 'Delete subject records', 'SUBJECT', 'DELETE'),
    ('SUBJECT_LIST', 'List all subjects', 'SUBJECT', 'LIST');

-- Attendance permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('ATTENDANCE_CREATE', 'Mark attendance', 'ATTENDANCE', 'CREATE'),
    ('ATTENDANCE_READ', 'View attendance records', 'ATTENDANCE', 'READ'),
    ('ATTENDANCE_UPDATE', 'Update attendance records', 'ATTENDANCE', 'UPDATE'),
    ('ATTENDANCE_DELETE', 'Delete attendance records', 'ATTENDANCE', 'DELETE'),
    ('ATTENDANCE_LIST', 'List attendance records', 'ATTENDANCE', 'LIST'),
    ('ATTENDANCE_EXPORT', 'Export attendance data', 'ATTENDANCE', 'EXPORT');

-- Examination permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('EXAMINATION_CREATE', 'Create examinations', 'EXAMINATION', 'CREATE'),
    ('EXAMINATION_READ', 'View examination information', 'EXAMINATION', 'READ'),
    ('EXAMINATION_UPDATE', 'Update examination information', 'EXAMINATION', 'UPDATE'),
    ('EXAMINATION_DELETE', 'Delete examination records', 'EXAMINATION', 'DELETE'),
    ('EXAMINATION_LIST', 'List all examinations', 'EXAMINATION', 'LIST');

-- Exam Results permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('EXAM_RESULT_CREATE', 'Create exam results', 'EXAM_RESULT', 'CREATE'),
    ('EXAM_RESULT_READ', 'View exam results', 'EXAM_RESULT', 'READ'),
    ('EXAM_RESULT_UPDATE', 'Update exam results', 'EXAM_RESULT', 'UPDATE'),
    ('EXAM_RESULT_DELETE', 'Delete exam results', 'EXAM_RESULT', 'DELETE'),
    ('EXAM_RESULT_LIST', 'List all exam results', 'EXAM_RESULT', 'LIST'),
    ('EXAM_RESULT_EXPORT', 'Export exam results', 'EXAM_RESULT', 'EXPORT');

-- Fee permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('FEE_CREATE', 'Create fee structures', 'FEE', 'CREATE'),
    ('FEE_READ', 'View fee information', 'FEE', 'READ'),
    ('FEE_UPDATE', 'Update fee structures', 'FEE', 'UPDATE'),
    ('FEE_DELETE', 'Delete fee records', 'FEE', 'DELETE'),
    ('FEE_LIST', 'List all fees', 'FEE', 'LIST');

-- Invoice permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('INVOICE_CREATE', 'Create invoices', 'INVOICE', 'CREATE'),
    ('INVOICE_READ', 'View invoices', 'INVOICE', 'READ'),
    ('INVOICE_UPDATE', 'Update invoices', 'INVOICE', 'UPDATE'),
    ('INVOICE_DELETE', 'Delete invoices', 'INVOICE', 'DELETE'),
    ('INVOICE_LIST', 'List all invoices', 'INVOICE', 'LIST'),
    ('INVOICE_EXPORT', 'Export invoice data', 'INVOICE', 'EXPORT');

-- Payment permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('PAYMENT_CREATE', 'Record payments', 'PAYMENT', 'CREATE'),
    ('PAYMENT_READ', 'View payment records', 'PAYMENT', 'READ'),
    ('PAYMENT_UPDATE', 'Update payment records', 'PAYMENT', 'UPDATE'),
    ('PAYMENT_DELETE', 'Delete payment records', 'PAYMENT', 'DELETE'),
    ('PAYMENT_LIST', 'List all payments', 'PAYMENT', 'LIST'),
    ('PAYMENT_EXPORT', 'Export payment data', 'PAYMENT', 'EXPORT'),
    ('PAYMENT_RECEIPT', 'Generate payment receipts', 'PAYMENT', 'RECEIPT');

-- Library permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('LIBRARY_BOOK_CREATE', 'Add books to library', 'LIBRARY_BOOK', 'CREATE'),
    ('LIBRARY_BOOK_READ', 'View book information', 'LIBRARY_BOOK', 'READ'),
    ('LIBRARY_BOOK_UPDATE', 'Update book information', 'LIBRARY_BOOK', 'UPDATE'),
    ('LIBRARY_BOOK_DELETE', 'Delete book records', 'LIBRARY_BOOK', 'DELETE'),
    ('LIBRARY_BOOK_LIST', 'List all books', 'LIBRARY_BOOK', 'LIST'),
    ('LIBRARY_BORROW_ISSUE', 'Issue books to borrowers', 'LIBRARY_BORROW', 'ISSUE'),
    ('LIBRARY_BORROW_RETURN', 'Process book returns', 'LIBRARY_BORROW', 'RETURN'),
    ('LIBRARY_BORROW_READ', 'View borrow records', 'LIBRARY_BORROW', 'READ');

-- Assignment permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('ASSIGNMENT_CREATE', 'Create assignments', 'ASSIGNMENT', 'CREATE'),
    ('ASSIGNMENT_READ', 'View assignments', 'ASSIGNMENT', 'READ'),
    ('ASSIGNMENT_UPDATE', 'Update assignments', 'ASSIGNMENT', 'UPDATE'),
    ('ASSIGNMENT_DELETE', 'Delete assignments', 'ASSIGNMENT', 'DELETE'),
    ('ASSIGNMENT_LIST', 'List all assignments', 'ASSIGNMENT', 'LIST'),
    ('ASSIGNMENT_SUBMIT', 'Submit assignments', 'ASSIGNMENT', 'SUBMIT'),
    ('ASSIGNMENT_GRADE', 'Grade assignments', 'ASSIGNMENT', 'GRADE');

-- Academic Year permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('ACADEMIC_YEAR_CREATE', 'Create academic years', 'ACADEMIC_YEAR', 'CREATE'),
    ('ACADEMIC_YEAR_READ', 'View academic years', 'ACADEMIC_YEAR', 'READ'),
    ('ACADEMIC_YEAR_UPDATE', 'Update academic years', 'ACADEMIC_YEAR', 'UPDATE'),
    ('ACADEMIC_YEAR_DELETE', 'Delete academic years', 'ACADEMIC_YEAR', 'DELETE'),
    ('ACADEMIC_YEAR_LIST', 'List all academic years', 'ACADEMIC_YEAR', 'LIST');

-- Term permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('TERM_CREATE', 'Create terms', 'TERM', 'CREATE'),
    ('TERM_READ', 'View terms', 'TERM', 'READ'),
    ('TERM_UPDATE', 'Update terms', 'TERM', 'UPDATE'),
    ('TERM_DELETE', 'Delete terms', 'TERM', 'DELETE'),
    ('TERM_LIST', 'List all terms', 'TERM', 'LIST');

-- Department permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('DEPARTMENT_CREATE', 'Create departments', 'DEPARTMENT', 'CREATE'),
    ('DEPARTMENT_READ', 'View department information', 'DEPARTMENT', 'READ'),
    ('DEPARTMENT_UPDATE', 'Update department information', 'DEPARTMENT', 'UPDATE'),
    ('DEPARTMENT_DELETE', 'Delete department records', 'DEPARTMENT', 'DELETE'),
    ('DEPARTMENT_LIST', 'List all departments', 'DEPARTMENT', 'LIST');

-- Timetable permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('TIMETABLE_CREATE', 'Create timetables', 'TIMETABLE', 'CREATE'),
    ('TIMETABLE_READ', 'View timetables', 'TIMETABLE', 'READ'),
    ('TIMETABLE_UPDATE', 'Update timetables', 'TIMETABLE', 'UPDATE'),
    ('TIMETABLE_DELETE', 'Delete timetables', 'TIMETABLE', 'DELETE'),
    ('TIMETABLE_LIST', 'List all timetables', 'TIMETABLE', 'LIST');

-- Payroll permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('PAYROLL_CREATE', 'Create payroll records', 'PAYROLL', 'CREATE'),
    ('PAYROLL_READ', 'View payroll records', 'PAYROLL', 'READ'),
    ('PAYROLL_UPDATE', 'Update payroll records', 'PAYROLL', 'UPDATE'),
    ('PAYROLL_DELETE', 'Delete payroll records', 'PAYROLL', 'DELETE'),
    ('PAYROLL_LIST', 'List all payroll records', 'PAYROLL', 'LIST'),
    ('PAYROLL_EXPORT', 'Export payroll data', 'PAYROLL', 'EXPORT');

-- Transport permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('TRANSPORT_ROUTE_CREATE', 'Create transport routes', 'TRANSPORT_ROUTE', 'CREATE'),
    ('TRANSPORT_ROUTE_READ', 'View transport routes', 'TRANSPORT_ROUTE', 'READ'),
    ('TRANSPORT_ROUTE_UPDATE', 'Update transport routes', 'TRANSPORT_ROUTE', 'UPDATE'),
    ('TRANSPORT_ROUTE_DELETE', 'Delete transport routes', 'TRANSPORT_ROUTE', 'DELETE'),
    ('TRANSPORT_ROUTE_LIST', 'List all transport routes', 'TRANSPORT_ROUTE', 'LIST');

-- Hostel permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('HOSTEL_CREATE', 'Create hostels', 'HOSTEL', 'CREATE'),
    ('HOSTEL_READ', 'View hostel information', 'HOSTEL', 'READ'),
    ('HOSTEL_UPDATE', 'Update hostel information', 'HOSTEL', 'UPDATE'),
    ('HOSTEL_DELETE', 'Delete hostel records', 'HOSTEL', 'DELETE'),
    ('HOSTEL_LIST', 'List all hostels', 'HOSTEL', 'LIST');

-- Event permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('EVENT_CREATE', 'Create events', 'EVENT', 'CREATE'),
    ('EVENT_READ', 'View events', 'EVENT', 'READ'),
    ('EVENT_UPDATE', 'Update events', 'EVENT', 'UPDATE'),
    ('EVENT_DELETE', 'Delete events', 'EVENT', 'DELETE'),
    ('EVENT_LIST', 'List all events', 'EVENT', 'LIST');

-- Notification permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('NOTIFICATION_CREATE', 'Create notifications', 'NOTIFICATION', 'CREATE'),
    ('NOTIFICATION_READ', 'View notifications', 'NOTIFICATION', 'READ'),
    ('NOTIFICATION_UPDATE', 'Update notifications', 'NOTIFICATION', 'UPDATE'),
    ('NOTIFICATION_DELETE', 'Delete notifications', 'NOTIFICATION', 'DELETE'),
    ('NOTIFICATION_LIST', 'List all notifications', 'NOTIFICATION', 'LIST');

-- Audit Log permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('AUDIT_LOG_READ', 'View audit logs', 'AUDIT_LOG', 'READ'),
    ('AUDIT_LOG_LIST', 'List all audit logs', 'AUDIT_LOG', 'LIST'),
    ('AUDIT_LOG_EXPORT', 'Export audit logs', 'AUDIT_LOG', 'EXPORT');

-- Inventory permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('INVENTORY_CREATE', 'Create inventory items', 'INVENTORY', 'CREATE'),
    ('INVENTORY_READ', 'View inventory items', 'INVENTORY', 'READ'),
    ('INVENTORY_UPDATE', 'Update inventory items', 'INVENTORY', 'UPDATE'),
    ('INVENTORY_DELETE', 'Delete inventory items', 'INVENTORY', 'DELETE'),
    ('INVENTORY_LIST', 'List all inventory items', 'INVENTORY', 'LIST');

-- School Info permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('SCHOOL_INFO_READ', 'View school information', 'SCHOOL_INFO', 'READ'),
    ('SCHOOL_INFO_UPDATE', 'Update school information', 'SCHOOL_INFO', 'UPDATE');

-- User Management permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('USER_CREATE', 'Create new users', 'USER', 'CREATE'),
    ('USER_READ', 'View user information', 'USER', 'READ'),
    ('USER_UPDATE', 'Update user information', 'USER', 'UPDATE'),
    ('USER_DELETE', 'Delete user records', 'USER', 'DELETE'),
    ('USER_LIST', 'List all users', 'USER', 'LIST'),
    ('USER_MANAGE_ROLES', 'Manage user roles', 'USER', 'MANAGE_ROLES'),
    ('USER_RESET_PASSWORD', 'Reset user passwords', 'USER', 'RESET_PASSWORD');

-- Role & Permission Management
INSERT INTO permissions (name, description, resource, action) VALUES
    ('ROLE_CREATE', 'Create roles', 'ROLE', 'CREATE'),
    ('ROLE_READ', 'View roles', 'ROLE', 'READ'),
    ('ROLE_UPDATE', 'Update roles', 'ROLE', 'UPDATE'),
    ('ROLE_DELETE', 'Delete roles', 'ROLE', 'DELETE'),
    ('ROLE_LIST', 'List all roles', 'ROLE', 'LIST'),
    ('PERMISSION_MANAGE', 'Manage permissions', 'PERMISSION', 'MANAGE');

-- Dashboard permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('DASHBOARD_VIEW_ADMIN', 'View admin dashboard', 'DASHBOARD', 'VIEW_ADMIN'),
    ('DASHBOARD_VIEW_TEACHER', 'View teacher dashboard', 'DASHBOARD', 'VIEW_TEACHER'),
    ('DASHBOARD_VIEW_STUDENT', 'View student dashboard', 'DASHBOARD', 'VIEW_STUDENT'),
    ('DASHBOARD_VIEW_PARENT', 'View parent dashboard', 'DASHBOARD', 'VIEW_PARENT'),
    ('DASHBOARD_VIEW_ACCOUNTANT', 'View accountant dashboard', 'DASHBOARD', 'VIEW_ACCOUNTANT'),
    ('DASHBOARD_VIEW_REPORTS', 'View reports dashboard', 'DASHBOARD', 'VIEW_REPORTS');

-- =============================================================================
-- INITIAL DATA: SUPER ADMIN USER
-- Password: admin123 (BCrypt hash)
-- =============================================================================
INSERT INTO users (username, email, password_hash, first_name, last_name, is_enabled, is_account_non_locked, created_at, updated_at)
VALUES ('admin', 'admin@sms.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System', 'Administrator', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- ASSIGN SUPER_ADMIN ROLE TO ADMIN USER
-- =============================================================================
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'SUPER_ADMIN';

-- =============================================================================
-- ASSIGN ALL PERMISSIONS TO SUPER_ADMIN ROLE
-- =============================================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN';

-- =============================================================================
-- INITIAL DATA: SCHOOL INFO
-- =============================================================================
INSERT INTO school_info (name, address, phone, email, website, motto, created_at, updated_at)
VALUES (
    'School Management System',
    '123 Education Street, Learning City, LC 12345',
    '+1-234-567-8900',
    'info@school.com',
    'www.school.com',
    'Knowledge is Power',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- DONE
-- =============================================================================
