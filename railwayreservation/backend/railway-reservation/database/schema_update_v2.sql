USE railway_reservation;

-- Remove fields no longer used by the CLI passenger module.
ALTER TABLE passengers DROP COLUMN IF EXISTS phone;
ALTER TABLE passengers DROP COLUMN IF EXISTS email;

-- Passenger keeps the latest PNR for quick display.
ALTER TABLE passengers ADD COLUMN IF NOT EXISTS pnr VARCHAR(20);

-- Keep train number unique; train_id remains the internal database primary key.
-- available_seats is the current free-seat count; total_seats is the fixed capacity.
