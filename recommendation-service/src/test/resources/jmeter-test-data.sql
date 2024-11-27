-- Insert test users (separate statements)
INSERT INTO users (user_id, username, password, role)
VALUES ('1', 'user1', '$2a$10$IHCNMfSEGv9kUOdDwcCJtevVkC4HF/hD44dVSdvqFXOnJNtUpudK.', 'ROLE_USER');

INSERT INTO users (user_id, username, password, role)
VALUES ('2', 'user2', '$2a$10$2pFbcO2kZ8UBz3IiNvhTjuidRyPGWqMhB1/qI/mYzP9nFEoK0r8Ly', 'ROLE_USER');

-- Insert test profiles (separate statements)
INSERT INTO profiles (user_id, email, firstname, lastname, phone, created_at, updated_at)
VALUES ('1', 'user1@example.com', 'User', 'One', '123-456-7890', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO profiles (user_id, email, firstname, lastname, phone, created_at, updated_at)
VALUES ('2', 'user2@example.com', 'User', 'Two', '098-765-4321', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test products (separate statements)
INSERT INTO products (parent_asin, title, price, average_rating, rating_number, category)
VALUES ('B00001', 'Test Product 1', 99.99, 4.5, 100, 'Electronics');

INSERT INTO products (parent_asin, title, price, average_rating, rating_number, category)
VALUES ('B00002', 'Test Product 2', 149.99, 4.0, 80, 'Electronics');

INSERT INTO products (parent_asin, title, price, average_rating, rating_number, category)
VALUES ('B00003', 'Test Product 3', 199.99, 4.8, 120, 'Electronics');

INSERT INTO products (parent_asin, title, price, average_rating, rating_number, category)
VALUES ('B00004', 'Test Product 4', 29.99, 4.2, 90, 'Books');

INSERT INTO products (parent_asin, title, price, average_rating, rating_number, category)
VALUES ('B00005', 'Test Product 5', 39.99, 4.6, 110, 'Books');

-- Insert general recommendations (separate statements)
INSERT INTO recommendations (user_id, product_list)
VALUES ('1', '["B00001", "B00002", "B00003"]');

INSERT INTO recommendations (user_id, product_list)
VALUES ('2', '["B00002", "B00003", "B00004"]');

-- Insert category-specific recommendations (separate statements)
INSERT INTO category_recommendations (category, user_id, product_list)
VALUES ('Electronics', '1', '["B00001", "B00002"]');

INSERT INTO category_recommendations (category, user_id, product_list)
VALUES ('Electronics', '2', '["B00002", "B00003"]');

INSERT INTO category_recommendations (category, user_id, product_list)
VALUES ('Books', '1', '["B00004", "B00005"]');

INSERT INTO category_recommendations (category, user_id, product_list)
VALUES ('Books', '2', '["B00004", "B00005"]');