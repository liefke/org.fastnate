-- Create Tablespace
-- Replace '...' with the path to you DB data directory, for example something with ...\app\oracle\oradata\XE\
CREATE TABLESPACE fastnate datafile '...\fastnate.dbf' SIZE 50M AUTOEXTEND ON NEXT 50M;

-- Create user fastnate
CREATE USER fastnate IDENTIFIED BY fastnate DEFAULT TABLESPACE fastnate TEMPORARY TABLESPACE TEMP QUOTA unlimited ON fastnate;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW to fastnate;
