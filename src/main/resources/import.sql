-- Locations --
INSERT INTO location (id, version, name) VALUES
('00000000-0000-4000-a000-000000000001', 0, 'Prague'),
('00000000-0000-4000-a000-000000000002', 0, 'Brno'),
('00000000-0000-4000-a000-000000000003', 0, 'Ostrava'),
('00000000-0000-4000-a000-000000000004', 0, 'Pilsen'),
('00000000-0000-4000-a000-000000000005', 0, 'Liberec'),
('00000000-0000-4000-a000-000000000006', 0, 'Olomouc'),
('00000000-0000-4000-a000-000000000007', 0, 'České Budějovice'),
('00000000-0000-4000-a000-000000000008', 0, 'Hradec Králové'),
('00000000-0000-4000-a000-000000000009', 0, 'Zlín'),
('00000000-0000-4000-a000-000000000010', 0, 'Karlovy Vary');

-- Categories --
INSERT INTO categories (id, version, name) VALUES
('00000000-0000-4000-b000-000000000001', 0, 'Haircut'),
('00000000-0000-4000-b000-000000000002', 0, 'Legal Consultation'),
('00000000-0000-4000-b000-000000000003', 0, 'Tax Filing'),
('00000000-0000-4000-b000-000000000004', 0, 'Skin Care Treatment'),
('00000000-0000-4000-b000-000000000005', 0, 'Laptop Repair'),
('00000000-0000-4000-b000-000000000006', 0, 'Phone Screen Replacement'),
('00000000-0000-4000-b000-000000000007', 0, 'Resume Review'),
('00000000-0000-4000-b000-000000000008', 0, 'Business Registration Help'),
('00000000-0000-4000-b000-000000000009', 0, 'Makeup Session'),
('00000000-0000-4000-b000-000000000010', 0, 'Photo Editing'),
('00000000-0000-4000-b000-000000000011', 0, 'Interior Design Consultation'),
('00000000-0000-4000-b000-000000000012', 0, 'Translation of Documents'),
('00000000-0000-4000-b000-000000000013', 0, 'Tutoring in Math'),
('00000000-0000-4000-b000-000000000014', 0, 'Guitar Lessons'),
('00000000-0000-4000-b000-000000000015', 0, 'Logo Design'),
('00000000-0000-4000-b000-000000000016', 0, 'Website Development'),
('00000000-0000-4000-b000-000000000017', 0, 'Plumbing Repair'),
('00000000-0000-4000-b000-000000000018', 0, 'Furniture Assembly'),
('00000000-0000-4000-b000-000000000019', 0, 'Pet Grooming'),
('00000000-0000-4000-b000-000000000020', 0, 'Fitness Coaching');

-- Users (Professionals) IDs 100–109
INSERT INTO users (id, version, email, password, first_name, last_name, phone_number, location_id, role, createat) VALUES
('00000000-0000-4000-8000-000000000100', 0, 'spec1@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Eva',    'Kralova',    '555555555', '00000000-0000-4000-a000-000000000001', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000101', 0, 'spec2@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Tomas',  'Dvorak',     '444444444', '00000000-0000-4000-a000-000000000002', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000102', 0, 'spec3@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Lucie',  'Horakova',   '111222333', '00000000-0000-4000-a000-000000000001', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000103', 0, 'spec4@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Petr',   'Novotny',    '222333444', '00000000-0000-4000-a000-000000000002', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000104', 0, 'spec5@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Karel',  'Sedlak',     '333444555', '00000000-0000-4000-a000-000000000001', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000105', 0, 'spec6@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Monika', 'Urbanova',   '444555666', '00000000-0000-4000-a000-000000000002', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000106', 0, 'spec7@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Jana',   'Prochazkova','555666777', '00000000-0000-4000-a000-000000000001', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000107', 0, 'spec8@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Martin', 'Cerny',      '666777888', '00000000-0000-4000-a000-000000000002', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000108', 0, 'spec9@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Alena',  'Kozlova',    '777888999', '00000000-0000-4000-a000-000000000001', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000109', 0, 'spec10@example.com','$2a$10$abcdefghijklmnopqrstuv', 'Marek',  'Blaha',      '888999000', '00000000-0000-4000-a000-000000000002', 'PROFESSIONAL', now());

-- Professional data
INSERT INTO professional (id, average_rating, description) VALUES
('00000000-0000-4000-8000-000000000100', 4.7, 'Experienced hair stylist with over 10 years in the business.'),
('00000000-0000-4000-8000-000000000101', 4.2, 'Certified tax consultant helping small businesses.'),
('00000000-0000-4000-8000-000000000102', 4.5, 'Expert in legal consulting and personal finance.'),
('00000000-0000-4000-8000-000000000103', 4.3, 'Tech enthusiast offering laptop and phone repair.'),
('00000000-0000-4000-8000-000000000104', 4.6, 'Haircuts and grooming for men and women.'),
('00000000-0000-4000-8000-000000000105', 4.1, 'Professional resume and job application advisor.'),
('00000000-0000-4000-8000-000000000106', 4.4, 'Skincare professional with years of experience.'),
('00000000-0000-4000-8000-000000000107', 4.0, 'Document translation and business help services.'),
('00000000-0000-4000-8000-000000000108', 4.8, 'Photo editing and logo design expert.'),
('00000000-0000-4000-8000-000000000109', 4.9, 'Website development and IT consulting.');

-- Professional ⟷ Categories
INSERT INTO professional_categories (professional_id, category_id) VALUES
('00000000-0000-4000-8000-000000000100', '00000000-0000-4000-b000-000000000001'), ('00000000-0000-4000-8000-000000000100', '00000000-0000-4000-b000-000000000004'),
('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-b000-000000000003'), ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-b000-000000000002'),
('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-b000-000000000002'), ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-b000-000000000003'),
('00000000-0000-4000-8000-000000000103', '00000000-0000-4000-b000-000000000005'), ('00000000-0000-4000-8000-000000000103', '00000000-0000-4000-b000-000000000006'),
('00000000-0000-4000-8000-000000000104', '00000000-0000-4000-b000-000000000001'), ('00000000-0000-4000-8000-000000000104', '00000000-0000-4000-b000-000000000003'),
('00000000-0000-4000-8000-000000000105', '00000000-0000-4000-b000-000000000007'), ('00000000-0000-4000-8000-000000000105', '00000000-0000-4000-b000-000000000002'),
('00000000-0000-4000-8000-000000000106', '00000000-0000-4000-b000-000000000004'), ('00000000-0000-4000-8000-000000000106', '00000000-0000-4000-b000-000000000001'),
('00000000-0000-4000-8000-000000000107', '00000000-0000-4000-b000-000000000008'), ('00000000-0000-4000-8000-000000000107', '00000000-0000-4000-b000-000000000012'),
('00000000-0000-4000-8000-000000000108', '00000000-0000-4000-b000-000000000010'), ('00000000-0000-4000-8000-000000000108', '00000000-0000-4000-b000-000000000015'),
('00000000-0000-4000-8000-000000000109', '00000000-0000-4000-b000-000000000016'), ('00000000-0000-4000-8000-000000000109', '00000000-0000-4000-b000-000000000008');

-- Users (Customers) IDs 110–111
INSERT INTO users (id, version, email, password, first_name, last_name, phone_number, location_id, role, createat) VALUES
('00000000-0000-4000-8000-000000000110', 0, 'customer1@example.com', '$2a$10$examplehashcustomer1', 'Anna',  'Novak',  '111111111', '00000000-0000-4000-a000-000000000001', 'CUSTOMER', now()),
('00000000-0000-4000-8000-000000000111', 0, 'customer2@example.com', '$2a$10$examplehashcustomer2', 'David', 'Svoboda','222222222', '00000000-0000-4000-a000-000000000002', 'CUSTOMER', now());

-- Customer data
INSERT INTO customer (id) VALUES
('00000000-0000-4000-8000-000000000110'),
('00000000-0000-4000-8000-000000000111');

-- Orders IDs 120–125
INSERT INTO orders (id, customer_id, status, description, price, review_id, createat, deadline, location_id) VALUES
(120, '00000000-0000-4000-8000-000000000110', 'CREATED', 'I need a haircut before my wedding.',               0, NULL, now(), now() + interval '5 days', '00000000-0000-4000-a000-000000000001'),
(121, '00000000-0000-4000-8000-000000000110', 'CREATED', 'My phone screen is cracked. Need a repair ASAP.',   0, NULL, now(), now() + interval '2 days', '00000000-0000-4000-a000-000000000002'),
(122, '00000000-0000-4000-8000-000000000111', 'CREATED', 'I need help with tax filing for my small business.',0, NULL, now(), now() + interval '10 days', '00000000-0000-4000-a000-000000000002'),
(123, '00000000-0000-4000-8000-000000000111', 'CREATED', 'Looking for a logo design for my startup.',         0, NULL, now(), now() + interval '7 days', '00000000-0000-4000-a000-000000000001'),
(124, '00000000-0000-4000-8000-000000000110', 'CREATED', 'Need a website for my online portfolio.',           0, NULL, now(), now() + interval '14 days', '00000000-0000-4000-a000-000000000001'),
(125, '00000000-0000-4000-8000-000000000111', 'CREATED', 'Interior design help for my apartment.',            0, NULL, now(), now() + interval '12 days', '00000000-0000-4000-a000-000000000001');

-- Orders ⟷ Categories
INSERT INTO orders_categories (order_id, category_id) VALUES
(120, '00000000-0000-4000-b000-000000000001'),   -- Haircut
(121, '00000000-0000-4000-b000-000000000006'),   -- Phone Screen Replacement
(122, '00000000-0000-4000-b000-000000000003'),   -- Tax Filing
(123, '00000000-0000-4000-b000-000000000015'),  -- Logo Design
(124, '00000000-0000-4000-b000-000000000016'),  -- Website Development
(125, '00000000-0000-4000-b000-000000000011');  -- Interior Design Consultation
