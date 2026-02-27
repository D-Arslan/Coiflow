-- Unique constraint: one transaction per appointment (prevents double-cashing)
CREATE UNIQUE INDEX idx_transaction_appointment ON transactions(appointment_id);
