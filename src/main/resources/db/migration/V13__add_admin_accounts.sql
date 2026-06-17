CREATE TABLE admins (
  id CHAR(36) NOT NULL,
  email VARCHAR(255) NOT NULL,
  login_id VARCHAR(15) NULL,
  nickname VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  temporary_password_hash VARCHAR(255) NULL,
  temporary_password_issued_at DATETIME(6) NULL,
  temporary_credential_expires_at DATETIME(6) NULL,
  password_hash VARCHAR(255) NULL,
  password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
  otp_registered BOOLEAN NOT NULL DEFAULT FALSE,
  otp_registration_required BOOLEAN NOT NULL DEFAULT TRUE,
  login_failure_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6) NULL,
  otp_failure_count INT NOT NULL DEFAULT 0,
  otp_blocked_until DATETIME(6) NULL,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NULL,
  updated_at DATETIME(6) NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admins_email (email),
  UNIQUE KEY uk_admins_login_id (login_id),
  KEY idx_admins_status_created_at (status, created_at),
  KEY idx_admins_temporary_credential_expires_at (temporary_credential_expires_at)
);

CREATE TABLE admin_roles (
  admin_id CHAR(36) NOT NULL,
  role VARCHAR(30) NOT NULL,
  PRIMARY KEY (admin_id),
  KEY idx_admin_roles_role (role),
  CONSTRAINT fk_admin_roles_admin_id
    FOREIGN KEY (admin_id)
    REFERENCES admins (id)
);
