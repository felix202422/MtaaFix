-- ============================================================================
-- Demo accounts: one login per role. Shared password: password123
-- (same BCrypt hash as the seeded admin account — change before real use)
--
-- Apply AFTER schema.sql + data.sql:
--   psql -U postgres -d sms_db -f backend/src/main/resources/db/demo-accounts.sql
-- Idempotent: safe to re-run.
-- ============================================================================

-- 1) Users (role account -> users row)
INSERT INTO users (username, email, password_hash, first_name, last_name)
VALUES
  ('demo.admin',      'demo.admin@sms.test',      '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Dana',   'Wells'),
  ('demo.teacher',    'demo.teacher@sms.test',    '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Elena',  'Ruiz'),
  ('demo.student',    'demo.student@sms.test',    '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Milo',   'Tan'),
  ('demo.parent',     'demo.parent@sms.test',     '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Iris',   'Tan'),
  ('demo.accountant', 'demo.accountant@sms.test', '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Omar',   'Reed'),
  ('demo.librarian',  'demo.librarian@sms.test',  '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Lena',   'Cross'),
  ('demo.registrar',  'demo.registrar@sms.test',  '$2a$12$IrvoXaTavhMIE3/12qlb1.KnlmV6pOBh2b.iWSwmI5IttGbEVJmaG', 'Ravi',   'Shah')
ON CONFLICT (username) DO UPDATE
  SET password_hash = EXCLUDED.password_hash, is_enabled = true, is_account_non_locked = true;

-- 2) Role assignments
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u
JOIN (VALUES
  ('demo.admin',      'SUPER_ADMIN'),
  ('demo.teacher',    'TEACHER'),
  ('demo.student',    'STUDENT'),
  ('demo.parent',     'PARENT'),
  ('demo.accountant', 'ACCOUNTANT'),
  ('demo.librarian',  'LIBRARIAN'),
  ('demo.registrar',  'REGISTRAR')
) AS m(username, role) ON true
JOIN roles r ON r.name = m.role
WHERE u.username = m.username
ON CONFLICT DO NOTHING;

-- 3) Teacher profile + teaching load (Maths + English in Grade 1, current year)
INSERT INTO teachers (user_id, employee_number, qualification, department_id, hire_date, employment_status)
SELECT u.id, 'TCH9001', 'B.Ed Mathematics',
       (SELECT id FROM departments ORDER BY id LIMIT 1), DATE '2020-01-13', 'ACTIVE'
FROM users u WHERE u.username = 'demo.teacher'
ON CONFLICT (employee_number) DO NOTHING;

INSERT INTO teacher_subjects (teacher_id, subject_id, class_id, academic_year_id)
SELECT t.id, s.id, c.id, ay.id
FROM users u
JOIN teachers t  ON t.user_id = u.id
JOIN classes  c  ON c.id IN (1, 2)
JOIN subjects s  ON s.id IN (SELECT id FROM subjects ORDER BY id LIMIT 2)
CROSS JOIN (SELECT id FROM academic_years WHERE is_current = true LIMIT 1) ay
WHERE u.username = 'demo.teacher'
ON CONFLICT DO NOTHING;

-- 4) Student profile (Grade 1, class 1)
INSERT INTO students (user_id, admission_number, admission_date, gender, current_class_id, academic_status)
SELECT u.id, 'STU99000001', DATE '2025-01-13', 'MALE', 1, 'ACTIVE'
FROM users u WHERE u.username = 'demo.student'
ON CONFLICT (admission_number) DO NOTHING;

-- 5) Parent profile + two linked children (the demo student + one seeded sibling)
CREATE UNIQUE INDEX IF NOT EXISTS ux_parents_user_id ON parents(user_id);

INSERT INTO parents (user_id, relationship, occupation)
SELECT u.id, 'GUARDIAN', 'Software Engineer'
FROM users u WHERE u.username = 'demo.parent'
ON CONFLICT (user_id) DO NOTHING;

UPDATE students s
SET parent_id = (SELECT p.id FROM parents p JOIN users u ON u.id = p.user_id WHERE u.username = 'demo.parent')
WHERE s.admission_number IN ('STU99000001', (SELECT MIN(admission_number) FROM students WHERE admission_number LIKE 'STU2025%'));

-- 6) Sample attendance + results for the demo student (last 10 attendance days)
INSERT INTO attendance (student_id, class_id, date, status)
SELECT s.id, s.current_class_id, d::date,
       CASE WHEN EXTRACT(DAY FROM d)::int % 7 = 0 THEN 'ABSENT' ELSE 'PRESENT' END
FROM students s
CROSS JOIN generate_series(CURRENT_DATE - INTERVAL '13 days', CURRENT_DATE, INTERVAL '1 day') d
WHERE s.admission_number = 'STU99000001'
  AND EXTRACT(ISODOW FROM d) < 6
  AND NOT EXISTS (SELECT 1 FROM attendance a WHERE a.student_id = s.id AND a.date = d::date);

INSERT INTO exam_results (student_id, subject_id, examination_id, marks_obtained, grade)
SELECT s.id, sub.id, e.id, 60 + (sub.id * 7) % 35,
       CASE WHEN 60 + (sub.id * 7) % 35 >= 85 THEN 'A'
            WHEN 60 + (sub.id * 7) % 35 >= 75 THEN 'B+'
            WHEN 60 + (sub.id * 7) % 35 >= 65 THEN 'B'
            ELSE 'C+' END
FROM students s
CROSS JOIN (SELECT id FROM subjects ORDER BY id LIMIT 4) sub
CROSS JOIN (SELECT id FROM examinations ORDER BY id DESC LIMIT 1) e
WHERE s.admission_number = 'STU99000001'
  AND NOT EXISTS (SELECT 1 FROM exam_results x
                  WHERE x.student_id = s.id AND x.subject_id = sub.id AND x.examination_id = e.id);

-- 7) Fee invoice + payment for the demo student
INSERT INTO invoices (student_id, invoice_number, total_amount, paid_amount, due_date, status)
SELECT s.id, 'INV-DEMO-0001', 450.00, 200.00, CURRENT_DATE + 20, 'PARTIALLY_PAID'
FROM students s WHERE s.admission_number = 'STU99000001'
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_number = 'INV-DEMO-0001');

INSERT INTO payments (student_id, invoice_id, payment_number, amount, payment_method, payment_date)
SELECT s.id, i.id, 'PAY-DEMO-0001', 200.00, 'BANK_TRANSFER', CURRENT_DATE
FROM students s JOIN invoices i ON i.invoice_number = 'INV-DEMO-0001'
WHERE s.admission_number = 'STU99000001'
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.payment_number = 'PAY-DEMO-0001');

-- ============================================================================
-- Expected logins (password: password123)
--   demo.admin / demo.teacher / demo.student / demo.parent
--   demo.accountant / demo.librarian / demo.registrar
-- ============================================================================
