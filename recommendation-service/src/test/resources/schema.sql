# src/test/resources/schema.sql
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(100) PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(100) DEFAULT 'password'
    );

CREATE TABLE IF NOT EXISTS products (
    parent_asin VARCHAR(50) PRIMARY KEY,
    title TEXT,
    price FLOAT,
    average_rating FLOAT,
    rating_number INT,
    category VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS recommendations (
    user_id VARCHAR(100) PRIMARY KEY,
    product_list CLOB
    );

CREATE TABLE IF NOT EXISTS category_recommendations (
    category VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    product_list CLOB,
    PRIMARY KEY (category, user_id)
    );