CREATE TABLE system_metadata (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_metadata_key (metadata_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO system_metadata (metadata_key, metadata_value)
VALUES ('schema_version', 'v0.1');
