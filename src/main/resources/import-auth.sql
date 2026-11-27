-- Users of the auth database; their profiles are in import-core.sql (same IDs) --

-- Users (Professionals) IDs 100–109
INSERT INTO users (id, version, email, password, role, createat) VALUES
('00000000-0000-4000-8000-000000000100', 0, 'spec1@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000101', 0, 'spec2@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000102', 0, 'spec3@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000103', 0, 'spec4@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000104', 0, 'spec5@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000105', 0, 'spec6@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000106', 0, 'spec7@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000107', 0, 'spec8@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000108', 0, 'spec9@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now()),
('00000000-0000-4000-8000-000000000109', 0, 'spec10@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'PROFESSIONAL', now());

-- Users (Customers) IDs 110–111
INSERT INTO users (id, version, email, password, role, createat) VALUES
('00000000-0000-4000-8000-000000000110', 0, 'customer1@example.com', '$2a$10$examplehashcustomer1', 'CUSTOMER', now()),
('00000000-0000-4000-8000-000000000111', 0, 'customer2@example.com', '$2a$10$examplehashcustomer2', 'CUSTOMER', now());
