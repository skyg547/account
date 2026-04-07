-- 내부통제/감사/권한 (Audit & Security) 스키마

-- 1. 시스템 사용자
CREATE TABLE system_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 2. 보안 역할
CREATE TABLE security_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 3. 시스템 기능/메뉴
CREATE TABLE system_functions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    func_code VARCHAR(50) NOT NULL UNIQUE,
    func_name VARCHAR(100) NOT NULL,
    parent_func_code VARCHAR(50),
    url_pattern VARCHAR(255),
    display_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 4. 역할별 기능 권한
CREATE TABLE role_func_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    func_id BIGINT NOT NULL,
    can_read BOOLEAN NOT NULL DEFAULT TRUE,
    can_write BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_approve BOOLEAN NOT NULL DEFAULT FALSE,
    data_range VARCHAR(50), -- ALL, OWN_DEPT, OWN_ONLY
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (role_id) REFERENCES security_roles(id),
    FOREIGN KEY (func_id) REFERENCES system_functions(id),
    UNIQUE (role_id, func_id)
);

-- 5. 사용자-역할 매핑
CREATE TABLE user_role_mappings (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES system_users(id),
    FOREIGN KEY (role_id) REFERENCES security_roles(id)
);

-- 6. 감사 로그 (Audit Trail)
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, LOGIN, APPROVE
    entity_name VARCHAR(100),
    entity_id VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    client_ip VARCHAR(50),
    description VARCHAR(255)
);

-- 7. 중요 이벤트 로그 (재처리/재오픈/지급 등)
CREATE TABLE critical_event_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(50) NOT NULL,
    source_system VARCHAR(50),
    status VARCHAR(30),
    request_data TEXT,
    response_data TEXT,
    error_msg TEXT,
    audit_user VARCHAR(50)
);
