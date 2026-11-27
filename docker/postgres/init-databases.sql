-- One database per module of the application (core, auth, messaging). The server runs this file once,
-- when its data directory is empty. The tables are created by the application, not here.
CREATE DATABASE core_db;
CREATE DATABASE auth_db;
CREATE DATABASE messaging_db;
