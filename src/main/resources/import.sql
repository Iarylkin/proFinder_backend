-- Locations --
INSERT INTO location (name) VALUES
('Prague'),
('Brno'),
('Ostrava'),
('Pilsen'),
('Liberec'),
('Olomouc'),
('České Budějovice'),
('Hradec Králové'),
('Zlín'),
('Karlovy Vary');

-- Services --
INSERT INTO service_offering (name) VALUES
('Haircut'),
('Legal Consultation'),
('Tax Filing'),
('Skin Care Treatment'),
('Laptop Repair'),
('Phone Screen Replacement'),
('Resume Review'),
('Business Registration Help'),
('Makeup Session'),
('Photo Editing'),
('Interior Design Consultation'),
('Translation of Documents'),
('Tutoring in Math'),
('Guitar Lessons'),
('Logo Design'),
('Website Development'),
('Plumbing Repair'),
('Furniture Assembly'),
('Pet Grooming'),
('Fitness Coaching');

-- Users (Specialists) IDs 100–109
INSERT INTO users (id, email, password, first_name, last_name, phone_number, location_id, role, createat) VALUES
(100, 'spec1@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Eva',    'Kralova',    '555555555', 1, 'SPECIALIST', now()),
(101, 'spec2@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Tomas',  'Dvorak',     '444444444', 2, 'SPECIALIST', now()),
(102, 'spec3@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Lucie',  'Horakova',   '111222333', 1, 'SPECIALIST', now()),
(103, 'spec4@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Petr',   'Novotny',    '222333444', 2, 'SPECIALIST', now()),
(104, 'spec5@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Karel',  'Sedlak',     '333444555', 1, 'SPECIALIST', now()),
(105, 'spec6@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Monika', 'Urbanova',   '444555666', 2, 'SPECIALIST', now()),
(106, 'spec7@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Jana',   'Prochazkova','555666777', 1, 'SPECIALIST', now()),
(107, 'spec8@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Martin', 'Cerny',      '666777888', 2, 'SPECIALIST', now()),
(108, 'spec9@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'Alena',  'Kozlova',    '777888999', 1, 'SPECIALIST', now()),
(109, 'spec10@example.com','$2a$10$abcdefghijklmnopqrstuv', 'Marek',  'Blaha',      '888999000', 2, 'SPECIALIST', now());

-- Specialist data
INSERT INTO specialist (id, average_rating, description) VALUES
(100, 4.7, 'Experienced hair stylist with over 10 years in the business.'),
(101, 4.2, 'Certified tax consultant helping small businesses.'),
(102, 4.5, 'Expert in legal consulting and personal finance.'),
(103, 4.3, 'Tech enthusiast offering laptop and phone repair.'),
(104, 4.6, 'Haircuts and grooming for men and women.'),
(105, 4.1, 'Professional resume and job application advisor.'),
(106, 4.4, 'Skincare specialist with years of experience.'),
(107, 4.0, 'Document translation and business help services.'),
(108, 4.8, 'Photo editing and logo design expert.'),
(109, 4.9, 'Website development and IT consulting.');

-- Specialist ⟷ Services
INSERT INTO specialist_service_offering (specialist_id, serviceofferings_id) VALUES
(100, 1), (100, 4),
(101, 3), (101, 2),
(102, 2), (102, 3),
(103, 5), (103, 6),
(104, 1), (104, 3),
(105, 7), (105, 2),
(106, 4), (106, 1),
(107, 8), (107, 12),
(108, 10), (108, 15),
(109, 16), (109, 8);

-- Users (Customers) IDs 110–111
INSERT INTO users (id, email, password, first_name, last_name, phone_number, location_id, role, createat) VALUES
(110, 'customer1@example.com', '$2a$10$examplehashcustomer1', 'Anna',  'Novak',  '111111111', 1, 'CUSTOMER', now()),
(111, 'customer2@example.com', '$2a$10$examplehashcustomer2', 'David', 'Svoboda','222222222', 2, 'CUSTOMER', now());

-- Customer data
INSERT INTO client (id) VALUES
(110),
(111);

-- Orders IDs 120–125
INSERT INTO orders (id, client_id, status, description, price, review_id, createat, deadline, location_id) VALUES
(120, 110, 'CREATED', 'I need a haircut before my wedding.',               0, NULL, now(), now() + interval '5 days',  1),
(121, 110, 'CREATED', 'My phone screen is cracked. Need a repair ASAP.',   0, NULL, now(), now() + interval '2 days',  2),
(122, 111, 'CREATED', 'I need help with tax filing for my small business.',0, NULL, now(), now() + interval '10 days', 2),
(123, 111, 'CREATED', 'Looking for a logo design for my startup.',         0, NULL, now(), now() + interval '7 days',  1),
(124, 110, 'CREATED', 'Need a website for my online portfolio.',           0, NULL, now(), now() + interval '14 days', 1),
(125, 111, 'CREATED', 'Interior design help for my apartment.',            0, NULL, now(), now() + interval '12 days', 1);

-- Orders ⟷ Services
INSERT INTO orders_service_offering (order_id, serviceofferings_id) VALUES
(120, 1),   -- Haircut
(121, 6),   -- Phone Screen Replacement
(122, 3),   -- Tax Filing
(123, 15),  -- Logo Design
(124, 16),  -- Website Development
(125, 11);  -- Interior Design Consultation
