-- Create database
CREATE DATABASE IF NOT EXISTS fastnate DEFAULT CHARACTER SET = utf8;

-- Create User 
GRANT USAGE ON *.* TO 'fastnate'@'localhost' IDENTIFIED BY 'fastnate';
GRANT ALL PRIVILEGES ON fastnate.* TO 'fastnate'@'localhost' WITH GRANT OPTION;
