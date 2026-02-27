-- =============================================
-- Coiflow - Initial Schema
-- =============================================

-- Salons
CREATE TABLE salons (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users (Single-Table Inheritance)
CREATE TABLE utilisateur (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    dtype           VARCHAR(31)  NOT NULL,
    salon_id        VARCHAR(36),
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    commission_rate DECIMAL(5,2),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_utilisateur_salon FOREIGN KEY (salon_id) REFERENCES salons(id)
);

CREATE INDEX idx_utilisateur_salon ON utilisateur(salon_id);
CREATE INDEX idx_utilisateur_email ON utilisateur(email);
CREATE INDEX idx_utilisateur_dtype ON utilisateur(dtype);

-- Clients
CREATE TABLE clients (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    salon_id    VARCHAR(36)  NOT NULL,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    notes       VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_client_salon FOREIGN KEY (salon_id) REFERENCES salons(id)
);

CREATE INDEX idx_client_salon ON clients(salon_id);

-- Services (prestations)
CREATE TABLE services (
    id               VARCHAR(36)   NOT NULL PRIMARY KEY,
    salon_id         VARCHAR(36)   NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    duration_minutes INT           NOT NULL,
    price            DECIMAL(10,2) NOT NULL,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_salon FOREIGN KEY (salon_id) REFERENCES salons(id)
);

CREATE INDEX idx_service_salon ON services(salon_id);

-- Appointments
CREATE TABLE appointments (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    salon_id    VARCHAR(36)  NOT NULL,
    barber_id   VARCHAR(36)  NOT NULL,
    client_id   VARCHAR(36),
    start_time  TIMESTAMP    NOT NULL,
    end_time    TIMESTAMP    NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    notes       VARCHAR(500),
    version     INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_salon  FOREIGN KEY (salon_id)  REFERENCES salons(id),
    CONSTRAINT fk_appointment_barber FOREIGN KEY (barber_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_appointment_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_appointment_time  CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_salon_start ON appointments(salon_id, start_time);
CREATE INDEX idx_appointment_barber_time ON appointments(barber_id, start_time, end_time);

-- Appointment <-> Services (many-to-many with price snapshot)
CREATE TABLE appointment_services (
    id              VARCHAR(36)   NOT NULL PRIMARY KEY,
    appointment_id  VARCHAR(36)   NOT NULL,
    service_id      VARCHAR(36)   NOT NULL,
    price_applied   DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_as_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_as_service     FOREIGN KEY (service_id)     REFERENCES services(id)
);

CREATE INDEX idx_as_appointment ON appointment_services(appointment_id);

-- Transactions
CREATE TABLE transactions (
    id              VARCHAR(36)   NOT NULL PRIMARY KEY,
    salon_id        VARCHAR(36)   NOT NULL,
    appointment_id  VARCHAR(36),
    barber_id       VARCHAR(36)   NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    created_by      VARCHAR(36)   NOT NULL,
    version         INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_salon       FOREIGN KEY (salon_id)       REFERENCES salons(id),
    CONSTRAINT fk_transaction_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_transaction_barber      FOREIGN KEY (barber_id)      REFERENCES utilisateur(id),
    CONSTRAINT fk_transaction_creator     FOREIGN KEY (created_by)     REFERENCES utilisateur(id)
);

CREATE INDEX idx_transaction_salon_created ON transactions(salon_id, created_at);

-- Payments (supports mixed payments per transaction)
CREATE TABLE payments (
    id              VARCHAR(36)   NOT NULL PRIMARY KEY,
    transaction_id  VARCHAR(36)   NOT NULL,
    method          VARCHAR(20)   NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_payment_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_transaction ON payments(transaction_id);

-- Commissions
CREATE TABLE commissions (
    id              VARCHAR(36)   NOT NULL PRIMARY KEY,
    salon_id        VARCHAR(36)   NOT NULL,
    barber_id       VARCHAR(36)   NOT NULL,
    transaction_id  VARCHAR(36)   NOT NULL,
    rate_applied    DECIMAL(5,2)  NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    period_start    DATE,
    period_end      DATE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commission_salon       FOREIGN KEY (salon_id)       REFERENCES salons(id),
    CONSTRAINT fk_commission_barber      FOREIGN KEY (barber_id)      REFERENCES utilisateur(id),
    CONSTRAINT fk_commission_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_commission_salon    ON commissions(salon_id);
CREATE INDEX idx_commission_barber   ON commissions(barber_id, created_at);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token ON refresh_tokens(token);
