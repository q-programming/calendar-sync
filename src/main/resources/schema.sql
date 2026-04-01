-- Schema for Spring Security OAuth2 Authorized Client persistence
CREATE TABLE IF NOT EXISTS oauth2_authorized_client (
  client_registration_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  access_token_type VARCHAR(100),
  access_token_value BLOB NOT NULL,
  access_token_issued_at TIMESTAMP,
  access_token_expires_at TIMESTAMP,
  access_token_scopes VARCHAR(1000),
  refresh_token_value BLOB,
  refresh_token_issued_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);

CREATE TABLE IF NOT EXISTS persistent_logins (
  username VARCHAR(64) NOT NULL,
  series VARCHAR(64) PRIMARY KEY,
  token VARCHAR(64) NOT NULL,
  last_used TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS profile_config (
  id BIGINT NOT NULL DEFAULT 1 PRIMARY KEY,
  outlook_profile_path VARCHAR(1000),
  outlook_calendar_id VARCHAR(500),
  outlook_calendar_name VARCHAR(500),
  google_calendar_id VARCHAR(500),
  google_calendar_name VARCHAR(500),
  google_principal_name VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sync_settings (
  id BIGINT NOT NULL DEFAULT 1 PRIMARY KEY,
  frequency_minutes INT NOT NULL DEFAULT 60,
  days_past INT NOT NULL DEFAULT 7,
  days_future INT NOT NULL DEFAULT 30,
  debug_logging BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS sync_run (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  started_at TIMESTAMP NOT NULL,
  finished_at TIMESTAMP,
  status VARCHAR(20),
  created INT DEFAULT 0,
  updated INT DEFAULT 0,
  deleted INT DEFAULT 0,
  message VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS sync_log_entry (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  run_id VARCHAR(36) NOT NULL,
  entry_timestamp TIMESTAMP NOT NULL,
  level VARCHAR(10) NOT NULL,
  message VARCHAR(4000),
  FOREIGN KEY (run_id) REFERENCES sync_run(id)
);
