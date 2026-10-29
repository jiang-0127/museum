USE museum_reservation;

ALTER TABLE museum ADD COLUMN daily_capacity INT NOT NULL DEFAULT 1200 AFTER close_time;

UPDATE museum SET daily_capacity = CASE id
    WHEN 1 THEN 1200
    WHEN 2 THEN 1000
    WHEN 3 THEN 800
    ELSE daily_capacity END;

ALTER TABLE reservation MODIFY COLUMN slot_id BIGINT NULL;

ALTER TABLE reservation ADD COLUMN visit_date DATE NULL AFTER slot_id;

ALTER TABLE reservation ADD KEY idx_reservation_visit (museum_id, visit_date);

ALTER TABLE visitor MODIFY COLUMN slot_id BIGINT NULL;
