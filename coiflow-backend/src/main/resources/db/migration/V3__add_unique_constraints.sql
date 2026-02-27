-- Unique constraint: service name per salon (race condition safety net)
-- H2 does not support LOWER() in index expressions, so we use a plain unique index.
-- The service layer handles case-insensitive checks via existsBySalon_IdAndNameIgnoreCase.
CREATE UNIQUE INDEX idx_service_salon_name ON services(salon_id, name);
