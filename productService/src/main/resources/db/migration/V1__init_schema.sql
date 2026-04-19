CREATE TABLE product (
                         id BIGINT PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         price DECIMAL(10,2) NOT NULL,
                         description TEXT,
                         image_url VARCHAR(500)
);