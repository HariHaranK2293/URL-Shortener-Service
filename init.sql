-- Create tablespace
CREATE TABLESPACE url_shortener_ts
  DATAFILE '/opt/oracle/oradata/url_shortener.dbf'
  SIZE 100M
  AUTOEXTEND ON NEXT 10M
  MAXSIZE UNLIMITED;

-- Create user
CREATE USER urlshortener IDENTIFIED BY urlshortener123
  DEFAULT TABLESPACE url_shortener_ts
  TEMPORARY TABLESPACE temp
  QUOTA UNLIMITED ON url_shortener_ts;

-- Grant privileges
GRANT CREATE SESSION TO urlshortener;
GRANT CREATE TABLE TO urlshortener;
GRANT CREATE SEQUENCE TO urlshortener;
GRANT CREATE TRIGGER TO urlshortener;
GRANT UNLIMITED TABLESPACE TO urlshortener;

-- Connect as the new user
CONNECT urlshortener/urlshortener123;

-- Create URLS table
CREATE TABLE urls (
    id NUMBER(19) PRIMARY KEY,
    short_code VARCHAR2(10) NOT NULL UNIQUE,
    original_url CLOB NOT NULL,
    custom_alias VARCHAR2(100),
    click_count NUMBER(19) DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    is_active NUMBER(1) DEFAULT 1,
    description VARCHAR2(500),
    api_key VARCHAR2(50) NOT NULL,
    CONSTRAINT chk_active CHECK (is_active IN (0, 1))
);

-- Create indexes on URLS table
CREATE INDEX idx_short_code ON urls(short_code);
CREATE INDEX idx_custom_alias ON urls(custom_alias);
CREATE INDEX idx_created_at ON urls(created_at);
CREATE INDEX idx_expires_at ON urls(expires_at);
CREATE INDEX idx_api_key ON urls(api_key);

-- Create ANALYTICS table
CREATE TABLE analytics (
    id NUMBER(19) PRIMARY KEY,
    url_id NUMBER(19) NOT NULL,
    ip_address VARCHAR2(45),
    user_agent CLOB,
    referrer CLOB,
    clicked_at TIMESTAMP NOT NULL,
    country VARCHAR2(100),
    device_type VARCHAR2(50),
    CONSTRAINT fk_analytics_url FOREIGN KEY (url_id) REFERENCES urls(id)
);

-- Create indexes on ANALYTICS table
CREATE INDEX idx_analytics_url_id ON analytics(url_id);
CREATE INDEX idx_analytics_clicked_at ON analytics(clicked_at);

-- Create sequences for ID generation
CREATE SEQUENCE urls_seq
  START WITH 1
  INCREMENT BY 1
  NOCACHE;

CREATE SEQUENCE analytics_seq
  START WITH 1
  INCREMENT BY 1
  NOCACHE;

-- Create triggers for auto-increment
CREATE TRIGGER urls_trigger
  BEFORE INSERT ON urls
  FOR EACH ROW
  BEGIN
    SELECT urls_seq.NEXTVAL INTO :NEW.id FROM dual;
  END;
/

CREATE TRIGGER analytics_trigger
  BEFORE INSERT ON analytics
  FOR EACH ROW
  BEGIN
    SELECT analytics_seq.NEXTVAL INTO :NEW.id FROM dual;
  END;
/

-- Commit changes
COMMIT;
