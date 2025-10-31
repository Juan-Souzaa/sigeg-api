-- password: admin (BCrypt $2a$10$ouyJ8rJ7b1fL9X7gN4vCpeV3Q1q2zS7dP0mA1bE2c3d4e5f6g7h8.)
INSERT INTO users(username, password) VALUES ('admin', '$2a$10$OaUeB6lqNwO3m1wOtw6G8eZK7i3oX2iY0g2x5QpVtX5H6C8W9Y0Qm');
INSERT INTO user_roles(user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';


