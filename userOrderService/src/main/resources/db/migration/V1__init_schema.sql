CREATE TABLE store_user (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            email VARCHAR(255) UNIQUE NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            first_name VARCHAR(100) NOT NULL,
                            last_name VARCHAR(100) NOT NULL,
                            role VARCHAR(50) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_order (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            user_id UUID NOT NULL,
                            total_amount DECIMAL(10,2) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES store_user(id) ON DELETE CASCADE
);

CREATE TABLE order_item (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            order_id UUID NOT NULL,
                            product_id BIGINT NOT NULL,
                            quantity INT NOT NULL,
                            price_at_purchase DECIMAL(10,2) NOT NULL,
                            CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES user_order(id) ON DELETE CASCADE
);