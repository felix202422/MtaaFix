-- =============================================================================
-- School Management System (SMS) - Seed Data
-- =============================================================================
-- Populates the database with realistic sample data.
-- Run AFTER schema.sql.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. UPDATE SCHOOL INFO
-- =============================================================================
UPDATE school_info SET
    name = 'Bright Future Academy',
    motto = 'Shaping Tomorrow''s Leaders',
    website = 'www.brightfutureacademy.edu'
WHERE id = 1;

-- =============================================================================
-- 2. DEPARTMENTS
-- =============================================================================
INSERT INTO departments (name, code, description) VALUES
    ('Mathematics', 'MATH', 'Mathematics and Statistics Department'),
    ('Science', 'SCI', 'Science Department covering Biology, Chemistry and Physics'),
    ('English', 'ENG', 'English Language and Literature Department'),
    ('History & Geography', 'HG', 'History, Geography and Social Studies'),
    ('Languages', 'LANG', 'Modern and Classical Languages Department'),
    ('Computer Science', 'CS', 'Computer Science and Information Technology'),
    ('Arts & Music', 'ART', 'Visual Arts, Music and Performing Arts'),
    ('Physical Education', 'PE', 'Physical Education, Sports and Health'),
    ('Business Studies', 'BUS', 'Business, Economics and Accounting'),
    ('Religious Studies', 'REL', 'Religious and Moral Education'),
    ('Special Education', 'SPED', 'Special Educational Needs Department'),
    ('Guidance & Counseling', 'GUID', 'Guidance and Counseling Services');

-- =============================================================================
-- 3. ACADEMIC YEARS
-- =============================================================================
INSERT INTO academic_years (name, start_date, end_date, is_current) VALUES
    ('2025/2026', '2025-01-13', '2025-11-28', false),
    ('2026/2027', '2026-01-12', '2026-11-27', true);

-- =============================================================================
-- 4. TERMS
-- =============================================================================
INSERT INTO terms (academic_year_id, name, start_date, end_date, is_current) VALUES
    (1, 'Term 1', '2025-01-13', '2025-04-18', false),
    (1, 'Term 2', '2025-05-05', '2025-08-08', false),
    (1, 'Term 3', '2025-08-25', '2025-11-28', false),
    (2, 'Term 1', '2026-01-12', '2026-04-17', true),
    (2, 'Term 2', '2026-05-04', '2026-08-07', false),
    (2, 'Term 3', '2026-08-24', '2026-11-27', false);

-- =============================================================================
-- 5. SUBJECTS
-- =============================================================================
INSERT INTO subjects (name, code, department_id, is_compulsory) VALUES
    ('Mathematics', 'MATH01', 1, true),
    ('Additional Mathematics', 'MATH02', 1, false),
    ('Statistics', 'MATH03', 1, false),
    ('Biology', 'SCI01', 2, true),
    ('Chemistry', 'SCI02', 2, true),
    ('Physics', 'SCI03', 2, true),
    ('General Science', 'SCI04', 2, true),
    ('English Language', 'ENG01', 3, true),
    ('English Literature', 'ENG02', 3, false),
    ('Creative Writing', 'ENG03', 3, false),
    ('History', 'HG01', 4, true),
    ('Geography', 'HG02', 4, true),
    ('Social Studies', 'HG03', 4, true),
    ('Civics', 'HG04', 4, false),
    ('French', 'LANG01', 5, false),
    ('Spanish', 'LANG02', 5, false),
    ('German', 'LANG03', 5, false),
    ('Swahili', 'LANG04', 5, false),
    ('Mandarin', 'LANG05', 5, false),
    ('Computer Science', 'CS01', 6, true),
    ('ICT', 'CS02', 6, true),
    ('Programming', 'CS03', 6, false),
    ('Art & Design', 'ART01', 7, false),
    ('Music', 'ART02', 7, false),
    ('Drama', 'ART03', 7, false),
    ('Physical Education', 'PE01', 8, true),
    ('Health Education', 'PE02', 8, true),
    ('Business Studies', 'BUS01', 9, false),
    ('Economics', 'BUS02', 9, false),
    ('Accounting', 'BUS03', 9, false),
    ('Commerce', 'BUS04', 9, false),
    ('Religious Studies', 'REL01', 10, true),
    ('Moral Education', 'REL02', 10, true),
    ('Life Skills', 'SPED01', 11, false),
    ('Guidance & Counseling', 'GUID01', 12, false);

-- =============================================================================
-- 6. CLASSES (30 classes: Grade 1-12)
-- =============================================================================
INSERT INTO classes (name, section, department_id, academic_year_id, capacity, room_number) VALUES
    ('Grade 1', 'A', null, 2, 40, 'G101'),
    ('Grade 1', 'B', null, 2, 40, 'G102'),
    ('Grade 2', 'A', null, 2, 40, 'G201'),
    ('Grade 2', 'B', null, 2, 40, 'G202'),
    ('Grade 3', 'A', null, 2, 40, 'G301'),
    ('Grade 3', 'B', null, 2, 40, 'G302'),
    ('Grade 4', 'A', null, 2, 40, 'G401'),
    ('Grade 4', 'B', null, 2, 40, 'G402'),
    ('Grade 5', 'A', null, 2, 40, 'G501'),
    ('Grade 5', 'B', null, 2, 40, 'G502'),
    ('Grade 6', 'A', null, 2, 42, 'G601'),
    ('Grade 6', 'B', null, 2, 42, 'G602'),
    ('Grade 6', 'C', null, 2, 42, 'G603'),
    ('Grade 7', 'A', 2, 2, 42, 'S701'),
    ('Grade 7', 'B', 2, 2, 42, 'S702'),
    ('Grade 7', 'C', 2, 2, 42, 'S703'),
    ('Grade 8', 'A', 2, 2, 42, 'S801'),
    ('Grade 8', 'B', 2, 2, 42, 'S802'),
    ('Grade 8', 'C', 2, 2, 42, 'S803'),
    ('Grade 9', 'A', 2, 2, 45, 'S901'),
    ('Grade 9', 'B', 2, 2, 45, 'S902'),
    ('Grade 9', 'C', 2, 2, 45, 'S903'),
    ('Grade 10', 'A', 1, 2, 45, 'S1001'),
    ('Grade 10', 'B', 1, 2, 45, 'S1002'),
    ('Grade 10', 'C', 1, 2, 45, 'S1003'),
    ('Grade 11', 'A', 1, 2, 45, 'S1101'),
    ('Grade 11', 'B', 1, 2, 45, 'S1102'),
    ('Grade 12', 'A', 1, 2, 45, 'S1201'),
    ('Grade 12', 'B', 1, 2, 45, 'S1202'),
    ('Grade 12', 'C', 1, 2, 45, 'S1203');

-- =============================================================================
-- 7. STREAMS (one per class)
-- =============================================================================
INSERT INTO streams (name, class_id) VALUES
    ('Stream A', 1), ('Stream B', 2),
    ('Stream A', 3), ('Stream B', 4),
    ('Stream A', 5), ('Stream B', 6),
    ('Stream A', 7), ('Stream B', 8),
    ('Stream A', 9), ('Stream B', 10),
    ('Stream A', 11), ('Stream B', 12), ('Stream C', 13),
    ('Stream A', 14), ('Stream B', 15), ('Stream C', 16),
    ('Stream A', 17), ('Stream B', 18), ('Stream C', 19),
    ('Stream A', 20), ('Stream B', 21), ('Stream C', 22),
    ('Stream A', 23), ('Stream B', 24), ('Stream C', 25),
    ('Stream A', 26), ('Stream B', 27),
    ('Stream A', 28), ('Stream B', 29), ('Stream C', 30);

-- =============================================================================
-- 8. USERS (Bulk creation via DO block)
-- =============================================================================
DO $$
DECLARE
    first_names TEXT[] := ARRAY['James','Mary','John','Patricia','Robert','Jennifer','Michael','Linda','David','Elizabeth','William','Barbara','Richard','Susan','Joseph','Jessica','Thomas','Sarah','Christopher','Karen','Charles','Lisa','Daniel','Nancy','Matthew','Betty','Anthony','Margaret','Mark','Sandra','Donald','Ashley','Steven','Kimberly','Paul','Emily','Andrew','Donna','Joshua','Michelle','Kevin','Amanda','Brian','Dorothy','George','Melissa','Timothy','Deborah','Ronald','Stephanie','Edward','Rebecca','Jason','Sharon','Jeffrey','Laura','Ryan','Cynthia','Jacob','Kathleen','Gary','Amy','Nicholas','Angela','Eric','Shirley','Jonathan','Anna','Stephen','Brenda','Larry','Pamela','Justin','Emma','Scott','Nicole','Brandon','Helen','Benjamin','Samantha','Samuel','Katherine','Raymond','Christine','Gregory','Debra','Frank','Rachel','Alexander','Carolyn','Patrick','Janet','Jack','Catherine','Dennis','Maria','Jerry','Heather','Tyler','Diane','Aisha','Chinua','Kwame','Ngozi','Thabo','Fatima','Ahmed','Mei','Wei','Hiroshi','Yuki','Priya','Raj','Deepak','Sunita','Kofi','Amara','Zuri','Malik','Sariah','Kwesi','Akosua','Segun','Chioma','Musa'];

    last_names TEXT[] := ARRAY['Smith','Johnson','Williams','Brown','Jones','Garcia','Miller','Davis','Rodriguez','Martinez','Hernandez','Lopez','Gonzalez','Wilson','Anderson','Thomas','Taylor','Moore','Jackson','Martin','Lee','Perez','Thompson','White','Harris','Sanchez','Clark','Ramirez','Lewis','Robinson','Walker','Young','Allen','King','Wright','Scott','Torres','Nguyen','Hill','Flores','Green','Adams','Nelson','Baker','Hall','Rivera','Campbell','Mitchell','Carter','Roberts','Gomez','Phillips','Evans','Turner','Diaz','Parker','Cruz','Edwards','Collins','Reyes','Stewart','Morris','Morales','Murphy','Cook','Rogers','Gutierrez','Ortiz','Morgan','Cooper','Peterson','Bailey','Reed','Kelly','Howard','Ramos','Kim','Cox','Ward','Richardson','Watson','Brooks','Chavez','Wood','James','Bennett','Gray','Mendoza','Ruiz','Hughes','Price','Alvarez','Castillo','Sanders','Patel','Myers','Long','Ross','Foster','Jimenez','Ochieng','Okonkwo','Mensah','Nkosi','Singh','Chen','Wang','Tanaka','Okafor','Abebe','Thapa','Boggs'];

    pw_hash TEXT := '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
    fn INT; ln INT; fname TEXT; lname TEXT; uname TEXT; em TEXT; g TEXT;
    i INT; j INT; idx INT;
    base_year INT := 2026;
BEGIN
    -- School admin (user_id = 2)
    INSERT INTO users (username, email, password_hash, first_name, last_name, is_enabled, is_account_non_locked)
    VALUES ('sarah.johnson', 'sarah.johnson@brightfuture.edu', pw_hash, 'Sarah', 'Johnson', true, true);

    -- 60 teachers (user_id 3-62)
    FOR i IN 1..60 LOOP
        fn := ((i * 7) % 100) + 1;
        ln := ((i * 13) % 100) + 1;
        fname := first_names[fn];
        lname := last_names[ln];
        uname := LOWER('t.' || fname || '.' || lname || i);
        em := LOWER(fname || '.' || lname || '.' || i || '@brightfuture.edu');
        INSERT INTO users (username, email, password_hash, first_name, last_name, is_enabled, is_account_non_locked)
        VALUES (uname, em, pw_hash, fname, lname, true, true);
    END LOOP;

    -- 2000 students (user_id 63-2062)
    FOR i IN 1..2000 LOOP
        fn := ((i * 11) % 100) + 1;
        ln := ((i * 17) % 100) + 1;
        fname := first_names[fn];
        lname := last_names[ln];
        uname := LOWER('s.' || fname || '.' || lname || i);
        em := LOWER(fname || '.' || lname || '.' || i || '@student.bfa.edu');
        INSERT INTO users (username, email, password_hash, first_name, last_name, is_enabled, is_account_non_locked)
        VALUES (uname, em, pw_hash, fname, lname, true, true);
    END LOOP;

    -- 3500 parents (user_id 2063-5562)
    FOR i IN 1..3500 LOOP
        fn := ((i * 19) % 100) + 1;
        ln := ((i * 23) % 100) + 1;
        fname := first_names[fn];
        lname := last_names[ln];
        uname := LOWER('p.' || fname || '.' || lname || i);
        em := LOWER(fname || '.' || lname || '.' || i || '@mail.com');
        INSERT INTO users (username, email, password_hash, first_name, last_name, is_enabled, is_account_non_locked)
        VALUES (uname, em, pw_hash, fname, lname, true, true);
    END LOOP;

    -- Staff users (user_id 5563-5567)
    INSERT INTO users (username, email, password_hash, first_name, last_name, is_enabled, is_account_non_locked) VALUES
        ('mike.brown', 'mike.brown@brightfuture.edu', pw_hash, 'Michael', 'Brown', true, true),
        ('linda.davis', 'linda.davis@brightfuture.edu', pw_hash, 'Linda', 'Davis', true, true),
        ('robert.wilson', 'robert.wilson@brightfuture.edu', pw_hash, 'Robert', 'Wilson', true, true),
        ('mary.taylor', 'mary.taylor@brightfuture.edu', pw_hash, 'Mary', 'Taylor', true, true),
        ('james.moore', 'james.moore@brightfuture.edu', pw_hash, 'James', 'Moore', true, true);
END $$;

-- =============================================================================
-- 9. USER_ROLES ASSIGNMENTS
-- =============================================================================
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2); -- School admin

-- Teachers role (users 3-62)
INSERT INTO user_roles (user_id, role_id)
SELECT generate_series, 3 FROM generate_series(3, 62);

-- Students role (users 63-2062)
INSERT INTO user_roles (user_id, role_id)
SELECT generate_series, 4 FROM generate_series(63, 2062);

-- Parents role (users 2063-5562)
INSERT INTO user_roles (user_id, role_id)
SELECT generate_series, 5 FROM generate_series(2063, 5562);

-- Staff roles
INSERT INTO user_roles (user_id, role_id) VALUES (5563, 6); -- Accountant
INSERT INTO user_roles (user_id, role_id) VALUES (5564, 7); -- Librarian
INSERT INTO user_roles (user_id, role_id) VALUES (5565, 8); -- Registrar
INSERT INTO user_roles (user_id, role_id) VALUES (5566, 9); -- Receptionist
INSERT INTO user_roles (user_id, role_id) VALUES (5567, 1); -- Guidance (super admin override)

-- =============================================================================
-- 10. PARENTS (user_id 2063-5562)
-- =============================================================================
INSERT INTO parents (user_id, occupation, relationship, workplace, annual_income)
SELECT
    u.id,
    CASE (u.id % 8)
        WHEN 0 THEN 'Engineer'
        WHEN 1 THEN 'Teacher'
        WHEN 2 THEN 'Doctor'
        WHEN 3 THEN 'Accountant'
        WHEN 4 THEN 'Business Owner'
        WHEN 5 THEN 'Nurse'
        WHEN 6 THEN 'Lawyer'
        WHEN 7 THEN 'Civil Servant'
    END,
    CASE (u.id % 3)
        WHEN 0 THEN 'MOTHER'
        WHEN 1 THEN 'FATHER'
        WHEN 2 THEN 'GUARDIAN'
    END,
    CASE (u.id % 6)
        WHEN 0 THEN 'City Hospital'
        WHEN 1 THEN 'National Bank'
        WHEN 2 THEN 'Government Office'
        WHEN 3 THEN 'Tech Solutions Inc'
        WHEN 4 THEN 'Greenfield School'
        WHEN 5 THEN 'Self Employed'
    END,
    (40000 + (u.id % 20) * 5000 + (u.id % 7) * 3000)::DECIMAL(12,2)
FROM users u
WHERE u.id BETWEEN 2063 AND 5562;

-- =============================================================================
-- 11. TEACHERS (user_id 3-62)
-- =============================================================================
INSERT INTO teachers (user_id, employee_number, qualification, department_id, specialization, hire_date, employment_status, salary, experience_years)
SELECT
    u.id,
    'TCH' || LPAD((u.id - 2)::TEXT, 4, '0'),
    CASE (u.id % 5)
        WHEN 0 THEN 'PhD in Education'
        WHEN 1 THEN 'Master of Education'
        WHEN 2 THEN 'Bachelor of Education'
        WHEN 3 THEN 'PGDE'
        WHEN 4 THEN 'Master of Arts in Education'
    END,
    ((u.id - 3) % 12) + 1,
    CASE ((u.id - 3) % 12)
        WHEN 0 THEN 'Pure Mathematics'
        WHEN 1 THEN 'Biology'
        WHEN 2 THEN 'English Literature'
        WHEN 3 THEN 'World History'
        WHEN 4 THEN 'French'
        WHEN 5 THEN 'Software Engineering'
        WHEN 6 THEN 'Fine Arts'
        WHEN 7 THEN 'Sports Science'
        WHEN 8 THEN 'Business Management'
        WHEN 9 THEN 'Theology'
        WHEN 10 THEN 'Special Needs Education'
        WHEN 11 THEN 'Psychology'
    END,
    DATE '2020-01-15' + ((u.id - 3) * 30 || ' days')::INTERVAL,
    'ACTIVE',
    (30000 + ((u.id - 3) * 900) % 55000)::DECIMAL(12,2),
    2 + ((u.id - 3) * 7) % 24
FROM users u
WHERE u.id BETWEEN 3 AND 62;

-- =============================================================================
-- 12. STUDENTS (user_id 63-2062)
-- =============================================================================
DO $$
DECLARE
    i INT;
    class_id INT;
    stream_id INT;
    class_offset INT;
    grade INT;
    dob DATE;
    gender TEXT;
    blood TEXT;
    nationality TEXT;
    parent_offset INT;
    parent_ref INT;
BEGIN
    FOR i IN 1..2000 LOOP
        -- Assign class (30 classes, ~67 students each)
        class_id := ((i - 1) / 67) + 1;
        IF class_id > 30 THEN class_id := 30; END IF;

        -- Assign stream (matches class_id)
        stream_id := class_id;

        -- Calculate grade from class_id
        class_offset := class_id - 1;
        IF class_offset < 2 THEN grade := 1;
        ELSIF class_offset < 4 THEN grade := 2;
        ELSIF class_offset < 6 THEN grade := 3;
        ELSIF class_offset < 8 THEN grade := 4;
        ELSIF class_offset < 10 THEN grade := 5;
        ELSIF class_offset < 13 THEN grade := 6;
        ELSIF class_offset < 16 THEN grade := 7;
        ELSIF class_offset < 19 THEN grade := 8;
        ELSIF class_offset < 22 THEN grade := 9;
        ELSIF class_offset < 25 THEN grade := 10;
        ELSIF class_offset < 27 THEN grade := 11;
        ELSE grade := 12;
        END IF;

        -- DOB: Grade 1 -> ~6-7 years old (born 2019-2020), Grade 12 -> ~17-18 (born 2008-2009)
        dob := DATE '2026-01-01' - ((grade * 365) + (i % 365) + 180 || ' days')::INTERVAL;

        gender := CASE WHEN i % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END;

        blood := CASE (i % 8)
            WHEN 0 THEN 'A+' WHEN 1 THEN 'A-' WHEN 2 THEN 'B+'
            WHEN 3 THEN 'B-' WHEN 4 THEN 'AB+' WHEN 5 THEN 'AB-'
            WHEN 6 THEN 'O+' WHEN 7 THEN 'O-'
        END;

        nationality := CASE (i % 10)
            WHEN 0 THEN 'American' WHEN 1 THEN 'British' WHEN 2 THEN 'Canadian'
            WHEN 3 THEN 'Nigerian' WHEN 4 THEN 'Kenyan' WHEN 5 THEN 'South African'
            WHEN 6 THEN 'Indian' WHEN 7 THEN 'Ghanaian' WHEN 8 THEN 'Egyptian'
            WHEN 9 THEN 'Ethiopian'
        END;

        -- Parent assignment: siblings share parents
        -- Students 1-500 get unique parents 1-500
        -- Students 501-1000 share parents 1-500 (siblings with 1-500)
        -- Students 1001-1500 get unique parents 501-1000
        -- Students 1501-2000 share parents 501-1000
        IF i <= 500 THEN parent_ref := i;
        ELSIF i <= 1000 THEN parent_ref := i - 500;
        ELSIF i <= 1500 THEN parent_ref := i - 500;
        ELSE parent_ref := i - 1000;
        END IF;

        INSERT INTO students (
            user_id, admission_number, admission_date, date_of_birth,
            gender, nationality, blood_group, address,
            previous_school, medical_info, emergency_contact_name, emergency_contact_phone,
            parent_id, stream_id, current_class_id, academic_status
        ) VALUES (
            62 + i,
            'STU2025' || LPAD(i::TEXT, 5, '0'),
            DATE '2025-01-13' + ((i % 270) || ' days')::INTERVAL,
            dob,
            gender, nationality, blood,
            i || ' Sample Street, Cityville, State ' || (i % 100),
            CASE WHEN i % 5 = 0 THEN 'Previous Primary School' ELSE NULL END,
            CASE WHEN i % 7 = 0 THEN 'Asthma - requires inhaler' ELSE NULL END,
            'Emergency Contact ' || i,
            '+1-555-' || LPAD((1000 + i % 9000)::TEXT, 4, '0'),
            parent_ref, stream_id, class_id, 'ACTIVE'
        );
    END LOOP;
END $$;

-- =============================================================================
-- 13. STUDENT_CLASSES (enroll students in classes for current academic year)
-- =============================================================================
INSERT INTO student_classes (student_id, class_id, academic_year_id, enrollment_date)
SELECT s.id, s.current_class_id, 2, s.admission_date
FROM students s;

-- =============================================================================
-- 14. TEACHER_SUBJECTS (assign teachers to subjects and classes)
-- =============================================================================
DO $$
DECLARE
    t INT; subj INT; cls INT; cnt INT;
BEGIN
    -- Each teacher gets 1-3 subjects for 2-4 classes
    FOR t IN 1..60 LOOP
        cnt := 1 + (t * 3) % 3;
        FOR j IN 1..cnt LOOP
            subj := ((t * 7 + j * 13) % 35) + 1;
            cls := ((t * 5 + j * 11) % 30) + 1;
            BEGIN
                INSERT INTO teacher_subjects (teacher_id, subject_id, class_id, academic_year_id)
                VALUES (t, subj, cls, 2);
            EXCEPTION WHEN unique_violation THEN
                -- Skip duplicates
            END;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- 15. ATTENDANCE (5000 records)
-- =============================================================================
DO $$
DECLARE
    i INT; sid INT; cid INT; att_date DATE; statuses TEXT[] := ARRAY['PRESENT','PRESENT','PRESENT','PRESENT','PRESENT','PRESENT','PRESENT','ABSENT','SICK','LATE','EXCUSED'];
    tchr INT;
BEGIN
    FOR i IN 1..5000 LOOP
        sid := (i * 17 % 2000) + 1;
        cid := ((SELECT current_class_id FROM students WHERE id = sid) - 1) + 1;
        att_date := DATE '2026-01-15' + ((i * 3) % 180 || ' days')::INTERVAL;
        tchr := ((cid * 7) % 60) + 1;
        BEGIN
            INSERT INTO attendance (student_id, class_id, date, status, marked_by)
            VALUES (sid, cid, att_date, statuses[(i % 11) + 1], tchr);
        EXCEPTION WHEN unique_violation THEN
            -- Skip duplicate
        END;
    END LOOP;
END $$;

-- =============================================================================
-- 16. EXAMINATIONS
-- =============================================================================
INSERT INTO examinations (name, type, term_id, academic_year_id, start_date, end_date, max_marks, pass_marks, weight) VALUES
    ('CAT 1 - Term 1 2026', 'CAT', 4, 2, '2026-02-10', '2026-02-14', 30, 15, 10.00),
    ('Midterm Exam - Term 1 2026', 'MIDTERM', 4, 2, '2026-03-10', '2026-03-21', 70, 35, 30.00),
    ('End of Term 1 Exam 2026', 'FINAL', 4, 2, '2026-04-07', '2026-04-17', 100, 50, 40.00),
    ('CAT 1 - Term 2 2026', 'CAT', 5, 2, '2026-05-20', '2026-05-24', 30, 15, 10.00),
    ('Midterm Exam - Term 2 2026', 'MIDTERM', 5, 2, '2026-06-15', '2026-06-26', 70, 35, 30.00),
    ('End of Term 2 Exam 2026', 'FINAL', 5, 2, '2026-07-27', '2026-08-07', 100, 50, 40.00);

-- =============================================================================
-- 17. EXAM RESULTS (10000 records)
-- =============================================================================
DO $$
DECLARE
    i INT; exam_id INT; student_id INT; subject_id INT;
    marks DECIMAL(6,2); grade VARCHAR(5); teacher_id INT;
BEGIN
    FOR i IN 1..10000 LOOP
        exam_id := (i % 6) + 1;
        student_id := ((i * 13) % 2000) + 1;
        subject_id := ((i * 7) % 35) + 1;

        -- Generate marks with some variation
        marks := (30 + ((i * 17 + student_id * 11) % 70))::DECIMAL(6,2);
        IF marks >= 80 THEN grade := 'A';
        ELSIF marks >= 70 THEN grade := 'B+';
        ELSIF marks >= 60 THEN grade := 'B';
        ELSIF marks >= 50 THEN grade := 'C+';
        ELSIF marks >= 40 THEN grade := 'C';
        ELSIF marks >= 30 THEN grade := 'D+';
        ELSIF marks >= 20 THEN grade := 'D';
        ELSE grade := 'E';
        END IF;

        teacher_id := ((subject_id * 7 + exam_id * 3) % 60) + 1;

        BEGIN
            INSERT INTO exam_results (examination_id, student_id, subject_id, marks_obtained, grade, graded_by)
            VALUES (exam_id, student_id, subject_id, marks, grade, teacher_id);
        EXCEPTION WHEN unique_violation THEN
            -- Skip duplicate
        END;
    END LOOP;
END $$;

-- =============================================================================
-- 18. ASSIGNMENTS (50 assignments)
-- =============================================================================
INSERT INTO assignments (title, description, subject_id, class_id, teacher_id, academic_year_id, due_date, max_marks) VALUES
    ('Algebra Fundamentals', 'Solve linear equations and inequalities', 1, 23, 1, 2, '2026-02-20 23:59:59', 100),
    ('Cell Structure Essay', 'Write an essay on plant and animal cell structures', 4, 14, 5, 2, '2026-02-22 23:59:59', 100),
    ('Shakespeare Analysis', 'Analyze themes in Romeo and Juliet Act 2', 9, 26, 8, 2, '2026-02-25 23:59:59', 100),
    ('World War II Timeline', 'Create a detailed timeline of key WWII events', 11, 20, 12, 2, '2026-02-28 23:59:59', 100),
    ('French Vocabulary Quiz', 'Study and prepare for vocabulary test on food and dining', 15, 14, 15, 2, '2026-03-02 23:59:59', 100),
    ('HTML Basics', 'Create a personal webpage using HTML5', 20, 23, 18, 2, '2026-03-05 23:59:59', 100),
    ('Water Cycle Diagram', 'Draw and label the complete water cycle', 7, 5, 22, 2, '2026-03-08 23:59:59', 100),
    ('Music Theory: Scales', 'Practice and submit major and minor scales', 24, 11, 25, 2, '2026-03-10 23:59:59', 100),
    ('Business Plan Draft', 'Write a one-page business plan for a startup', 28, 26, 28, 2, '2026-03-12 23:59:59', 100),
    ('Physical Fitness Log', 'Keep a 2-week exercise and nutrition log', 26, 9, 31, 2, '2026-03-15 23:59:59', 100),
    ('Trigonometry Problems', 'Solve advanced trigonometric equations', 1, 28, 2, 2, '2026-03-18 23:59:59', 100),
    ('Chemical Reactions Lab', 'Document observations from chemical reaction experiments', 5, 17, 6, 2, '2026-03-20 23:59:59', 100),
    ('Poetry Composition', 'Write 3 original poems with different styles', 10, 12, 9, 2, '2026-03-22 23:59:59', 100),
    ('Map Reading Exercise', 'Complete topographic map interpretation worksheet', 12, 15, 13, 2, '2026-03-25 23:59:59', 100),
    ('Spanish Dialogue', 'Write and record a conversation in Spanish', 16, 21, 16, 2, '2026-03-28 23:59:59', 100),
    ('Database Design', 'Design an ER diagram for a library system', 21, 25, 19, 2, '2026-04-01 23:59:59', 100),
    ('Newton''s Laws Report', 'Describe real-world applications of Newton''s laws', 6, 18, 23, 2, '2026-04-03 23:59:59', 100),
    ('Still Life Drawing', 'Create a charcoal still life drawing', 23, 7, 26, 2, '2026-04-05 23:59:59', 100),
    ('Supply & Demand Analysis', 'Analyze market equilibrium case studies', 29, 26, 29, 2, '2026-04-08 23:59:59', 100),
    ('Group Dance Choreography', 'Choreograph and perform a 3-minute group dance', 26, 13, 32, 2, '2026-04-10 23:59:59', 100),
    ('Statistical Data Project', 'Collect and analyze data using statistical methods', 3, 28, 3, 2, '2026-04-12 23:59:59', 100),
    ('Genetics Punnett Squares', 'Complete genetic cross probability exercises', 4, 17, 5, 2, '2026-04-15 23:59:59', 100),
    ('Persuasive Essay', 'Write a persuasive essay on a current affairs topic', 8, 23, 10, 2, '2026-04-18 23:59:59', 100),
    ('Ancient Civilizations Comparison', 'Compare Egyptian and Mesopotamian civilizations', 11, 20, 12, 2, '2026-04-20 23:59:59', 100),
    ('German Grammar Exercises', 'Complete advanced German grammar worksheets', 17, 14, 17, 2, '2026-04-22 23:59:59', 100),
    ('Python Programming', 'Write a Python program for a calculator app', 22, 25, 20, 2, '2026-04-25 23:59:59', 100),
    ('Plant Identification', 'Identify and classify 20 local plant species', 7, 8, 22, 2, '2026-04-28 23:59:59', 100),
    ('Choir Performance Piece', 'Learn and record assigned choral piece', 24, 12, 25, 2, '2026-05-01 23:59:59', 100),
    ('Financial Accounting', 'Prepare financial statements from trial balance', 30, 27, 30, 2, '2026-05-05 23:59:59', 100),
    ('Sports Officiating Quiz', 'Study and pass rules test for assigned sport', 26, 16, 31, 2, '2026-05-08 23:59:59', 100),
    ('Calculus Derivatives', 'Solve derivative problems using chain rule', 1, 30, 2, 2, '2026-05-10 23:59:59', 100),
    ('Organic Chemistry Naming', 'Name organic compounds using IUPAC nomenclature', 5, 19, 6, 2, '2026-05-12 23:59:59', 100),
    ('Short Story Writing', 'Write a 2000-word original short story', 10, 15, 9, 2, '2026-05-15 23:59:59', 100),
    ('Climate Change Research', 'Research paper on climate change impacts in Africa', 12, 22, 13, 2, '2026-05-18 23:59:59', 100),
    ('Mandarin Characters', 'Practice and submit 50 Mandarin character writings', 19, 21, 21, 2, '2026-05-20 23:59:59', 100),
    ('Network Topology', 'Design a network topology for a school campus', 20, 25, 18, 2, '2026-05-22 23:59:59', 100),
    ('Electrical Circuits', 'Build and measure a series-parallel circuit', 6, 20, 23, 2, '2026-05-25 23:59:59', 100),
    ('Watercolor Landscape', 'Paint a landscape scene using watercolor techniques', 23, 11, 26, 2, '2026-05-28 23:59:59', 100),
    ('Marketing Plan', 'Develop a marketing plan for a new product', 28, 27, 28, 2, '2026-06-01 23:59:59', 100),
    ('Yoga and Meditation Log', 'Document 2 weeks of yoga practice sessions', 27, 9, 33, 2, '2026-06-05 23:59:59', 100),
    ('Probability Problems', 'Solve probability distribution problems', 3, 29, 3, 2, '2026-06-08 23:59:59', 100),
    ('Human Anatomy Model', 'Create a labeled 3D model of the human heart', 4, 18, 5, 2, '2026-06-10 23:59:59', 100),
    ('Media Analysis Essay', 'Analyze bias in news media coverage', 8, 26, 10, 2, '2026-06-12 23:59:59', 100),
    ('Swahili Conversation Practice', 'Record a conversation in Swahili', 18, 14, 34, 2, '2026-06-15 23:59:59', 100),
    ('JavaScript Game Project', 'Create a simple browser game using JavaScript', 22, 25, 20, 2, '2026-06-18 23:59:59', 100),
    ('Ecosystem Study', 'Document a local ecosystem food web', 7, 5, 22, 2, '2026-06-20 23:59:59', 100),
    ('Drama Monologue Performance', 'Perform and record a 2-minute monologue', 25, 11, 27, 2, '2026-06-22 23:59:59', 100),
    ('Economics Case Study', 'Analyze inflation trends in developing economies', 29, 27, 29, 2, '2026-06-25 23:59:59', 100),
    ('First Aid Certification', 'Complete first aid procedures assessment', 27, 13, 33, 2, '2026-06-28 23:59:59', 100),
    ('Portfolio Review', 'Compile and submit semester portfolio of best works', 8, 30, 10, 2, '2026-07-01 23:59:59', 100);

-- =============================================================================
-- 19. FEES (fee structure per class level)
-- =============================================================================
INSERT INTO fees (name, description, amount, fee_type, class_id, academic_year_id, term_id, is_mandatory) VALUES
    ('Tuition Fee - Primary', 'Primary school tuition (Grade 1-6)', 2500.00, 'TUITION', null, 2, 4, true),
    ('Tuition Fee - Lower Secondary', 'Lower secondary tuition (Grade 7-9)', 3500.00, 'TUITION', null, 2, 4, true),
    ('Tuition Fee - Upper Secondary', 'Upper secondary tuition (Grade 10-12)', 4500.00, 'TUITION', null, 2, 4, true),
    ('Development Levy', 'School infrastructure development', 500.00, 'LEVY', null, 2, 4, true),
    ('Library Fee', 'Library access and materials', 200.00, 'LIBRARY', null, 2, 4, true),
    ('Sports Fee', 'Sports and athletic activities', 150.00, 'SPORTS', null, 2, 4, true),
    ('ICT Fee', 'Computer lab and internet access', 300.00, 'ICT', null, 2, 4, true),
    ('Examination Fee', 'Term examination processing', 250.00, 'EXAM', null, 2, 4, true),
    ('Transport Fee', 'School bus transportation', 800.00, 'TRANSPORT', null, 2, 4, false),
    ('Boarding Fee', 'Hostel accommodation', 2000.00, 'BOARDING', null, 2, 4, false),
    ('Tuition Fee - Primary', 'Primary school tuition (Grade 1-6)', 2500.00, 'TUITION', null, 2, 5, true),
    ('Tuition Fee - Lower Secondary', 'Lower secondary tuition (Grade 7-9)', 3500.00, 'TUITION', null, 2, 5, true),
    ('Tuition Fee - Upper Secondary', 'Upper secondary tuition (Grade 10-12)', 4500.00, 'TUITION', null, 2, 5, true);

-- =============================================================================
-- 20. INVOICES (for students)
-- =============================================================================
DO $$
DECLARE
    i INT; sid INT; fee_id INT; amount DECIMAL(12,2); cls INT;
BEGIN
    FOR i IN 1..2000 LOOP
        sid := i;
        cls := (SELECT current_class_id FROM students WHERE id = sid);

        -- Determine fee based on class level
        IF cls <= 13 THEN
            fee_id := 1; -- Primary tuition
            amount := 2500.00;
        ELSIF cls <= 22 THEN
            fee_id := 2; -- Lower secondary
            amount := 3500.00;
        ELSE
            fee_id := 3; -- Upper secondary
            amount := 4500.00;
        END IF;

        -- Add mandatory fees
        amount := amount + 500 + 200 + 150 + 300 + 250; -- levy + library + sports + ict + exam

        -- Add transport for some students
        IF i % 4 = 0 THEN
            amount := amount + 800;
        END IF;

        INSERT INTO invoices (invoice_number, student_id, fee_id, total_amount, paid_amount, discount, due_date, status)
        VALUES (
            'INV-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(i::TEXT, 6, '0'),
            sid, fee_id, amount, 0, 0,
            DATE '2026-02-15', 'PENDING'
        );
    END LOOP;
END $$;

-- =============================================================================
-- 21. PAYMENTS (250 records)
-- =============================================================================
DO $$
DECLARE
    i INT; sid INT; inv_id INT; amt DECIMAL(12,2);
    methods TEXT[] := ARRAY['CASH','BANK_TRANSFER','MOBILE_MONEY','CHEQUE'];
    ref TEXT;
BEGIN
    FOR i IN 1..250 LOOP
        sid := ((i * 19) % 2000) + 1;
        inv_id := sid;
        amt := (SELECT total_amount FROM invoices WHERE id = inv_id) * (0.3 + (i % 5) * 0.1);

        -- Update invoice paid amount
        UPDATE invoices SET paid_amount = paid_amount + amt,
            status = CASE WHEN paid_amount + amt >= total_amount THEN 'PAID' ELSE 'PARTIAL' END
            WHERE id = inv_id;

        ref := UPPER(SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 12));

        INSERT INTO payments (payment_number, invoice_id, student_id, amount, payment_method, transaction_reference, payment_date, received_by, receipt_number)
        VALUES (
            'PAY-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(i::TEXT, 5, '0'),
            inv_id, sid, amt,
            methods[(i % 4) + 1],
            ref,
            DATE '2026-01-20' + ((i * 2) % 180 || ' days')::INTERVAL,
            2 + ((i * 7) % 60),
            'RCPT-' || LPAD(i::TEXT, 5, '0')
        );
    END LOOP;
END $$;

-- =============================================================================
-- 22. LIBRARY BOOKS (500 books)
-- =============================================================================
DO $$
DECLARE
    titles TEXT[] := ARRAY[
        'The Great Gatsby', 'To Kill a Mockingbird', '1984', 'Pride and Prejudice',
        'The Catcher in the Rye', 'Animal Farm', 'Lord of the Flies', 'Brave New World',
        'The Hobbit', 'Fahrenheit 451', 'Jane Eyre', 'Wuthering Heights',
        'Great Expectations', 'The Odyssey', 'Moby Dick', 'War and Peace',
        'Crime and Punishment', 'The Adventures of Huckleberry Finn', 'The Scarlet Letter',
        'Of Mice and Men', 'The Old Man and the Sea', 'A Tale of Two Cities',
        'The Grapes of Wrath', 'Catch-22', 'Slaughterhouse-Five', 'One Hundred Years of Solitude',
        'The Lord of the Rings', 'The Chronicles of Narnia', 'Harry Potter and the Philosopher''s Stone',
        'The Da Vinci Code', 'Angels and Demons', 'The Alchemist', 'The Kite Runner',
        'Life of Pi', 'The Book Thief', 'The Road', 'The Hunger Games',
        'Divergent', 'The Fault in Our Stars', 'Gone Girl', 'The Girl on the Train',
        'The Martian', 'Ready Player One', 'Dune', 'Foundation', 'Neuromancer',
        'Ender''s Game', 'The Handmaid''s Tale', 'Beloved', 'Things Fall Apart',
        'Half of a Yellow Sun', 'Americanah', 'Purple Hibiscus', 'The God of Small Things',
        'Midnight''s Children', 'The White Tiger', 'Interpreter of Maladies', 'The Namesake',
        'A Brief History of Time', 'The Selfish Gene', 'Origin of Species', 'The Double Helix',
        'Silent Spring', 'Cosmos', 'The Elegant Universe', 'Sapiens', 'Homo Deus',
        'Thinking Fast and Slow', 'Outliers', 'Freakonomics', 'The Tipping Point',
        'Guns Germs and Steel', 'The Power of Habit', 'Start With Why', 'Good to Great',
        'The 7 Habits of Highly Effective People', 'How to Win Friends and Influence People',
        'Think and Grow Rich', 'Rich Dad Poor Dad', 'The Intelligent Investor',
        'The Art of War', 'The Prince', 'Meditations', 'Thus Spoke Zarathustra',
        'Beyond Good and Evil', 'The Republic', 'Nicomachean Ethics', 'The Social Contract',
        'The Wealth of Nations', 'Das Kapital', 'The Communist Manifesto',
        'A People''s History of the United States', 'The Diary of a Young Girl',
        'Long Walk to Freedom', 'The Autobiography of Malcolm X', 'The Story of My Experiments with Truth',
        'Steve Jobs', 'Einstein', 'Leonardo da Vinci', 'Becoming', 'Educated'
    ];
    authors TEXT[] := ARRAY[
        'F. Scott Fitzgerald', 'Harper Lee', 'George Orwell', 'Jane Austen',
        'J.D. Salinger', 'William Golding', 'Aldous Huxley', 'J.R.R. Tolkien',
        'Ray Bradbury', 'Charlotte Bronte', 'Emily Bronte', 'Charles Dickens',
        'Homer', 'Herman Melville', 'Leo Tolstoy', 'Fyodor Dostoevsky',
        'Mark Twain', 'Nathaniel Hawthorne', 'John Steinbeck', 'Ernest Hemingway',
        'Joseph Heller', 'Kurt Vonnegut', 'Gabriel Garcia Marquez', 'C.S. Lewis',
        'J.K. Rowling', 'Dan Brown', 'Paulo Coelho', 'Khaled Hosseini',
        'Yann Martel', 'Markus Zusak', 'Cormac McCarthy', 'Suzanne Collins',
        'Veronica Roth', 'John Green', 'Gillian Flynn', 'Paula Hawkins',
        'Andy Weir', 'Ernest Cline', 'Frank Herbert', 'Isaac Asimov',
        'William Gibson', 'Orson Scott Card', 'Margaret Atwood', 'Toni Morrison',
        'Chinua Achebe', 'Chimamanda Ngozi Adichie', 'Arundhati Roy', 'Salman Rushdie',
        'Aravind Adiga', 'Jhumpa Lahiri', 'Stephen Hawking', 'Richard Dawkins',
        'James D. Watson', 'Rachel Carson', 'Carl Sagan', 'Brian Greene',
        'Yuval Noah Harari', 'Daniel Kahneman', 'Malcolm Gladwell', 'Steven Levitt',
        'Jared Diamond', 'Charles Duhigg', 'Simon Sinek', 'Jim Collins',
        'Stephen Covey', 'Dale Carnegie', 'Napoleon Hill', 'Robert Kiyosaki',
        'Benjamin Graham', 'Sun Tzu', 'Niccolo Machiavelli', 'Marcus Aurelius',
        'Friedrich Nietzsche', 'Plato', 'Aristotle', 'Jean-Jacques Rousseau',
        'Adam Smith', 'Karl Marx', 'Howard Zinn', 'Anne Frank',
        'Nelson Mandela', 'Walter Isaacson', 'Michelle Obama', 'Tara Westover'
    ];
    categories TEXT[] := ARRAY['Fiction','Non-Fiction','Science','History','Mathematics','Literature'];
    publishers TEXT[] := ARRAY['Penguin Books','Oxford University Press','Cambridge University Press','HarperCollins','Random House','Simon & Schuster','Macmillan','Scholastic'];
    i INT; title_idx INT; author_idx INT; cat_idx INT; pub_idx INT;
BEGIN
    FOR i IN 1..500 LOOP
        title_idx := (i % 100) + 1;
        author_idx := (i * 7 % 84) + 1;
        cat_idx := (i % 6) + 1;
        pub_idx := (i * 3 % 8) + 1;

        INSERT INTO library_books (title, author, isbn, publisher, category, edition, quantity, available_quantity, location, barcode, year_published)
        VALUES (
            titles[title_idx],
            authors[author_idx],
            '978-' || LPAD((1000000 + i)::TEXT, 10, '0'),
            publishers[pub_idx],
            categories[cat_idx],
            'Edition ' || ((i % 5) + 1),
            1 + (i % 5),
            1 + (i % 5),
            'Shelf-' || CHR(64 + ((i - 1) / 50)::INT) || '-' || LPAD(((i - 1) % 50 + 1)::TEXT, 3, '0'),
            'BRC-' || LPAD(i::TEXT, 5, '0'),
            1990 + (i % 35)
        );
    END LOOP;
END $$;

-- =============================================================================
-- 23. BORROW RECORDS
-- =============================================================================
DO $$
DECLARE
    i INT; book_id INT; borrower_id INT; due TIMESTAMP; ret TIMESTAMP; fine DECIMAL(8,2);
BEGIN
    FOR i IN 1..800 LOOP
        book_id := (i % 500) + 1;
        borrower_id := ((i * 11) % 2000) + 63; -- students as borrowers

        due := TIMESTAMP '2026-02-01 00:00:00' + ((i * 3) || ' days')::INTERVAL;

        -- Some returned, some still borrowed
        IF i % 3 = 0 THEN
            ret := due + ((i % 10) || ' days')::INTERVAL;
            fine := CASE WHEN i % 7 = 0 THEN (i % 5) * 2.50 ELSE 0 END;
        ELSE
            ret := NULL;
            fine := 0;
        END IF;

        INSERT INTO borrow_records (book_id, borrower_id, borrow_date, due_date, return_date, status, fine_amount, issued_by)
        VALUES (
            book_id, borrower_id,
            TIMESTAMP '2026-01-15 00:00:00' + ((i * 2) || ' days')::INTERVAL,
            due, ret,
            CASE WHEN ret IS NOT NULL THEN 'RETURNED' ELSE 'BORROWED' END,
            fine, 5564
        );
    END LOOP;
END $$;

-- =============================================================================
-- 24. INVENTORY (200 items)
-- =============================================================================
DO $$
DECLARE
    item_names TEXT[] := ARRAY[
        'Whiteboard Markers', 'Chalk Set', 'Desk Chair', 'Student Desk', 'Teacher Desk',
        'Whiteboard', 'Projector Lamp', 'Laptop Charger', 'Printer Toner', 'A4 Paper Ream',
        'Globe', 'Microscope Slide Set', 'Beaker Set', 'Test Tube Rack', 'Bunsen Burner',
        'Chemistry Stand', 'Spring Balance', 'Thermometer', 'Voltmeter', 'Ammeter',
        'Football', 'Basketball', 'Volleyball Net', 'Tennis Racket', 'Cricket Bat',
        'Chess Board', 'Art Easel', 'Paint Brush Set', 'Acrylic Paint Set', 'Sketch Pad',
        'Music Stand', 'Guitar Strings', 'Drum Sticks', 'Keyboard Stand', 'Sheet Music Book',
        'Library Shelf', 'Book Cart', 'Reading Table', 'Study Lamp', 'Bookend Set',
        'Fire Extinguisher', 'First Aid Kit', 'Safety Goggles', 'Lab Coat', 'Protective Gloves',
        'Floor Mop', 'Broom Set', 'Waste Bin', 'Cleaning Trolley', 'Disinfectant Spray'
    ];
    categories TEXT[] := ARRAY['CLASSROOM','LABORATORY','SPORTS','ARTS','MUSIC','LIBRARY','SAFETY','CLEANING'];
    suppliers TEXT[] := ARRAY['EduSupply Co','SchoolMart Inc','LabTech Industries','SportPro Ltd','ArtWorld Supplies','CleanCorp'];
    i INT; name_idx INT; cat_idx INT; sup_idx INT; qty INT; price DECIMAL(10,2);
BEGIN
    FOR i IN 1..200 LOOP
        name_idx := (i % 50) + 1;
        cat_idx := (i % 8) + 1;
        sup_idx := (i * 3 % 6) + 1;
        qty := 5 + (i * 7 % 95);
        price := (5 + (i * 13 % 295) + 0.99)::DECIMAL(10,2);

        INSERT INTO inventory (name, description, category, quantity, unit_price, supplier, purchase_date, location, status)
        VALUES (
            item_names[name_idx],
            item_names[name_idx] || ' for school use',
            categories[cat_idx],
            qty, price,
            suppliers[sup_idx],
            DATE '2025-01-01' + ((i * 2) % 365 || ' days')::INTERVAL,
            'Store Room ' || CHR(64 + (i % 6 + 1)),
            CASE WHEN qty > 10 THEN 'AVAILABLE' WHEN qty > 3 THEN 'LOW_STOCK' ELSE 'OUT_OF_STOCK' END
        );
    END LOOP;
END $$;

-- =============================================================================
-- 25. EVENTS (50 events)
-- =============================================================================
DO $$
DECLARE
    event_titles TEXT[] := ARRAY[
        'Annual Sports Day', 'Inter-Class Football Tournament', 'Basketball Championship',
        'School Swimming Gala', 'Athletics Meet', 'Cross Country Run',
        'Science Fair', 'Mathematics Olympiad', 'Spelling Bee Competition',
        'Debate Championship', 'Quiz Competition', 'Essay Writing Contest',
        'Art Exhibition', 'Music Concert', 'Drama Festival',
        'Talent Show', 'Cultural Day', 'International Food Fair',
        'Parents-Teachers Meeting', 'Open Day', 'Career Guidance Workshop',
        'Anti-Bullying Campaign', 'Environmental Clean-Up Day', 'Tree Planting Drive',
        'Field Trip - National Museum', 'Field Trip - Science Centre', 'Educational Tour',
        'Prize Giving Day', 'Graduation Ceremony', 'Speech Day',
        'Eid Celebration', 'Christmas Carol Service', 'Diwali Celebration',
        'New Year Festival', 'Independence Day Celebration', 'Africa Day Event',
        'First Aid Training', 'Fire Drill', 'Safety Awareness Week',
        'Computer Programming Workshop', 'Robotics Club Exhibition', 'Coding Hackathon',
        'Chess Tournament', 'Scouts Camping Trip', 'Charity Fundraiser',
        'Book Fair', 'Reading Week', 'Library Week Celebration',
        'Staff Professional Development', 'Board of Governors Meeting'
    ];
    event_types TEXT[] := ARRAY['SPORTS','ACADEMIC','ARTS','CULTURAL','FIELD_TRIP','CEREMONY','WORKSHOP','OTHER'];
    locations TEXT[] := ARRAY['School Main Hall','Sports Ground','Assembly Hall','Science Lab','Computer Lab','Art Studio','Library','School Field','Auditorium','Classroom Block A'];
    i INT; title_idx INT; type_idx INT; loc_idx INT;
BEGIN
    FOR i IN 1..50 LOOP
        title_idx := i;
        type_idx := ((i - 1) % 8) + 1;
        loc_idx := ((i * 3) % 10) + 1;

        INSERT INTO events (title, description, event_type, start_date, end_date, location, organized_by, is_public)
        VALUES (
            event_titles[title_idx],
            event_titles[title_idx] || ' - Annual school event for students and staff',
            event_types[type_idx],
            TIMESTAMP '2026-02-01 08:00:00' + ((i * 7) || ' days')::INTERVAL,
            TIMESTAMP '2026-02-01 08:00:00' + ((i * 7 + 1) || ' days')::INTERVAL,
            locations[loc_idx],
            2 + ((i * 5) % 60),
            i % 3 != 0
        );
    END LOOP;
END $$;

-- =============================================================================
-- 26. NOTIFICATIONS (100 notifications)
-- =============================================================================
DO $$
DECLARE
    notice_titles TEXT[] := ARRAY[
        'Exam Timetable Released', 'School Closed for Holiday', 'PTA Meeting Rescheduled',
        'New Library Books Available', 'Sports Practice Cancelled', 'Fee Payment Deadline',
        'School Trip Permission Slips', 'Academic Calendar Update', 'Staff Meeting Reminder',
        'COVID-19 Safety Guidelines', 'Uniform Policy Reminder', 'Scholarship Application Open',
        'Science Week Activities', 'Parent Workshop Registration', 'End of Term Reports Available',
        'Bus Route Changes', 'Lost and Found Items', 'School Photo Day',
        'Vaccination Drive', 'Emergency Closure Announcement'
    ];
    notice_types TEXT[] := ARRAY['ACADEMIC','EVENT','FEE','GENERAL','EMERGENCY'];
    priorities TEXT[] := ARRAY['LOW','NORMAL','HIGH','URGENT'];
    target_roles TEXT[] := ARRAY['TEACHER','STUDENT','PARENT','ALL'];
    i INT; title_idx INT; type_idx INT; prio_idx INT; role_idx INT;
BEGIN
    FOR i IN 1..100 LOOP
        title_idx := (i % 20) + 1;
        type_idx := (i % 5) + 1;
        prio_idx := (i % 4) + 1;
        role_idx := (i % 4) + 1;

        INSERT INTO notifications (title, message, type, sender_id, priority, is_read, target_role)
        VALUES (
            notice_titles[title_idx],
            notice_titles[title_idx] || ' - Please take note of this important announcement.',
            notice_types[type_idx],
            2,
            priorities[prio_idx],
            i % 3 = 0,
            target_roles[role_idx]
        );
    END LOOP;
END $$;

-- =============================================================================
-- 27. TIMETABLES (timetables for classes)
-- =============================================================================
DO $$
DECLARE
    days TEXT[] := ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'];
    start_hours INT[] := ARRAY[8,9,10,11,12,14,15];
    i INT; cls INT; subj INT; tchr INT; day TEXT; sh INT;
BEGIN
    FOR i IN 1..600 LOOP
        cls := ((i - 1) / 20) + 1;
        IF cls > 30 THEN EXIT; END IF;
        day := days[((i - 1) / 4) % 5 + 1];
        sh := start_hours[((i - 1) % 7) + 1];

        SELECT ts.subject_id, ts.teacher_id INTO subj, tchr
        FROM teacher_subjects ts
        WHERE ts.class_id = cls
        ORDER BY RANDOM() LIMIT 1;

        IF subj IS NOT NULL THEN
            INSERT INTO timetables (class_id, subject_id, teacher_id, day_of_week, start_time, end_time, room, academic_year_id, term_id)
            VALUES (
                cls, subj, tchr, day,
                (sh || ':00:00')::TIME,
                ((sh + 1) || ':00:00')::TIME,
                'RM-' || LPAD(cls::TEXT, 3, '0'),
                2, 4
            );
        END IF;
    END LOOP;
END $$;

-- =============================================================================
-- 28. PAYROLL (payroll records for teachers)
-- =============================================================================
DO $$
DECLARE
    i INT; t INT; basic DECIMAL(12,2); allow DECIMAL(12,2); deduct DECIMAL(12,2); tax DECIMAL(12,2); net DECIMAL(12,2);
BEGIN
    FOR i IN 1..360 LOOP
        t := ((i - 1) % 60) + 1;
        basic := (SELECT salary FROM teachers WHERE id = t);
        allow := (basic * 0.1)::DECIMAL(12,2);
        deduct := (basic * 0.05)::DECIMAL(12,2);
        tax := (basic * 0.08)::DECIMAL(12,2);
        net := (basic + allow - deduct - tax)::DECIMAL(12,2);

        INSERT INTO payroll (teacher_id, basic_salary, allowances, deductions, tax, net_salary, payment_date, month, year, status)
        VALUES (
            t, basic, allow, deduct, tax, net,
            DATE '2026-01-25' + (((i - 1) / 60) * 30 || ' days')::INTERVAL,
            ((i - 1) / 60) + 1,
            2026,
            'PAID'
        );
    END LOOP;
END $$;

-- =============================================================================
-- 29. TRANSPORT ROUTES
-- =============================================================================
INSERT INTO transport_routes (name, bus_number, driver_name, driver_phone, route_description, capacity, fare) VALUES
    ('Route 1 - North Side', 'BUS-001', 'John Kamau', '+254-712-345-678', 'Covers North residential areas: Sunrise Estate, Green Valley, Hilltop', 60, 800.00),
    ('Route 2 - East Side', 'BUS-002', 'Mary Wanjiku', '+254-723-456-789', 'Covers East residential areas: Riverside, Eastlands, Muthaiga', 55, 750.00),
    ('Route 3 - South Side', 'BUS-003', 'Peter Ochieng', '+254-734-567-890', 'Covers South residential areas: South B, Langata, Karen', 65, 850.00),
    ('Route 4 - West Side', 'BUS-004', 'Anne Muthoni', '+254-745-678-901', 'Covers West residential areas: Westlands, Kileleshwa, Lavington', 60, 800.00),
    ('Route 5 - City Centre', 'BUS-005', 'David Kimani', '+254-756-789-012', 'Covers downtown and city centre area', 50, 700.00);

-- =============================================================================
-- 30. TRANSPORT STUDENTS (assign students to routes)
-- =============================================================================
INSERT INTO transport_students (transport_route_id, student_id, pickup_point)
SELECT
    ((s.id - 1) % 5) + 1,
    s.id,
    CASE ((s.id - 1) % 5)
        WHEN 0 THEN 'Sunrise Estate Gate'
        WHEN 1 THEN 'Riverside Mall Stop'
        WHEN 2 THEN 'South B Shopping Centre'
        WHEN 3 THEN 'Westlands Bus Stop'
        WHEN 4 THEN 'City Centre Terminal'
    END
FROM students s
WHERE s.id % 4 = 0; -- 25% of students use transport

-- =============================================================================
-- 31. HOSTELS
-- =============================================================================
INSERT INTO hostels (name, description, capacity, warden_id) VALUES
    ('Bright Future Boys Hostel', 'Main boys dormitory block with 50 rooms', 100, 5),
    ('Bright Future Girls Hostel', 'Main girls dormitory block with 50 rooms', 100, 12),
    ('Junior Wings Hostel', 'Hostel for Grade 1-6 boarding students', 60, 18),
    ('Executive Hostel', 'Premium boarding with en-suite rooms for senior students', 40, 25);

-- =============================================================================
-- 32. HOSTEL ROOMS
-- =============================================================================
INSERT INTO hostel_rooms (hostel_id, room_number, capacity, occupied, room_type) VALUES
    (1, 'B101', 4, 2, 'DORMITORY'), (1, 'B102', 4, 3, 'DORMITORY'), (1, 'B103', 4, 4, 'DORMITORY'),
    (1, 'B104', 2, 1, 'SHARED'), (1, 'B105', 2, 2, 'SHARED'), (1, 'B106', 2, 0, 'SHARED'),
    (1, 'B107', 4, 3, 'DORMITORY'), (1, 'B108', 4, 2, 'DORMITORY'), (1, 'B109', 4, 1, 'DORMITORY'),
    (1, 'B110', 2, 2, 'SHARED'),
    (2, 'G101', 4, 3, 'DORMITORY'), (2, 'G102', 4, 2, 'DORMITORY'), (2, 'G103', 4, 4, 'DORMITORY'),
    (2, 'G104', 2, 1, 'SHARED'), (2, 'G105', 2, 0, 'SHARED'), (2, 'G106', 2, 2, 'SHARED'),
    (2, 'G107', 4, 3, 'DORMITORY'), (2, 'G108', 4, 2, 'DORMITORY'), (2, 'G109', 4, 1, 'DORMITORY'),
    (2, 'G110', 2, 2, 'SHARED'),
    (3, 'J01', 4, 2, 'DORMITORY'), (3, 'J02', 4, 3, 'DORMITORY'), (3, 'J03', 4, 1, 'DORMITORY'),
    (3, 'J04', 4, 2, 'DORMITORY'), (3, 'J05', 4, 0, 'DORMITORY'), (3, 'J06', 4, 3, 'DORMITORY'),
    (4, 'E01', 2, 1, 'ENSUITE'), (4, 'E02', 2, 2, 'ENSUITE'), (4, 'E03', 2, 0, 'ENSUITE'),
    (4, 'E04', 2, 1, 'ENSUITE'), (4, 'E05', 2, 2, 'ENSUITE');

-- =============================================================================
-- 33. HOSTEL STUDENTS (assign some students to hostels)
-- =============================================================================
INSERT INTO hostel_students (hostel_room_id, student_id, check_in_date, status)
SELECT
    ((s.id - 1) % 30) + 1,
    s.id,
    DATE '2026-01-12',
    'ACTIVE'
FROM students s
WHERE s.id % 5 = 0 AND s.id <= 600; -- 120 boarding students

-- =============================================================================
-- 34. DEPARTMENT HEADS (update department head_id)
-- =============================================================================
UPDATE departments SET head_id = 1 WHERE id = 1;
UPDATE departments SET head_id = 5 WHERE id = 2;
UPDATE departments SET head_id = 8 WHERE id = 3;
UPDATE departments SET head_id = 12 WHERE id = 4;
UPDATE departments SET head_id = 15 WHERE id = 5;
UPDATE departments SET head_id = 18 WHERE id = 6;
UPDATE departments SET head_id = 22 WHERE id = 7;
UPDATE departments SET head_id = 25 WHERE id = 8;
UPDATE departments SET head_id = 28 WHERE id = 9;
UPDATE departments SET head_id = 31 WHERE id = 10;
UPDATE departments SET head_id = 34 WHERE id = 11;
UPDATE departments SET head_id = 37 WHERE id = 12;

-- =============================================================================
-- 35. CLASS TEACHERS (update class class_teacher_id referencing user IDs of teachers 3-32)
-- =============================================================================
UPDATE classes SET class_teacher_id = userId FROM (SELECT id, (ROW_NUMBER() OVER (ORDER BY id) + 2) AS userId FROM classes) sub WHERE classes.id = sub.id;

-- =============================================================================
-- DONE
-- =============================================================================
COMMIT;
