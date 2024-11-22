DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS recommendations;
DROP TABLE IF EXISTS category_recommendations;

CREATE TABLE users (
                       user_id VARCHAR(100) PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL
);

CREATE TABLE profiles (
                          user_id VARCHAR(100) PRIMARY KEY,
                          email VARCHAR(255) NOT NULL,
                          firstname VARCHAR(100),
                          lastname VARCHAR(100),
                          phone VARCHAR(20),
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE products (
                          parent_asin VARCHAR(50) PRIMARY KEY,
                          title TEXT,
                          price FLOAT,
                          average_rating FLOAT,
                          rating_number INT,
                          category VARCHAR(50)
);

CREATE TABLE recommendations (
                                 user_id VARCHAR(100) PRIMARY KEY,
                                 product_list CLOB,
                                 FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE category_recommendations (
                                          category VARCHAR(50) NOT NULL,
                                          user_id VARCHAR(100) NOT NULL,
                                          product_list CLOB,
                                          PRIMARY KEY (category, user_id),
                                          FOREIGN KEY (user_id) REFERENCES users(user_id)
);