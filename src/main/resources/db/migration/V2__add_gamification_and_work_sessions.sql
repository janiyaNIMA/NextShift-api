-- V2__add_gamification_and_work_sessions.sql
-- NextShift Element Journey gamification & work sessions

CREATE TABLE IF NOT EXISTS element_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tier_code VARCHAR(50) NOT NULL UNIQUE,
    element_name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    mass_number INT NOT NULL,
    required_points INT NOT NULL UNIQUE,
    tier_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_element_tiers_points ON element_tiers(required_points);
CREATE INDEX IF NOT EXISTS idx_element_tiers_order ON element_tiers(tier_order);

CREATE TABLE IF NOT EXISTS student_gamification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE,
    total_points INT NOT NULL DEFAULT 0,
    total_verified_minutes BIGINT NOT NULL DEFAULT 0,
    current_element_tier_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_gamification_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_gamification_tier FOREIGN KEY (current_element_tier_id) REFERENCES element_tiers(id)
);

CREATE INDEX IF NOT EXISTS idx_gamification_points ON student_gamification(total_points);

CREATE TABLE IF NOT EXISTS work_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    verified_minutes BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    verified_by_id BIGINT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_work_sessions_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_work_sessions_verifier FOREIGN KEY (verified_by_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_work_sessions_booking_status ON work_sessions(booking_id, status);

CREATE TABLE IF NOT EXISTS point_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    work_session_id BIGINT NULL,
    points_delta INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_point_tx_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_point_tx_session FOREIGN KEY (work_session_id) REFERENCES work_sessions(id)
);

CREATE INDEX IF NOT EXISTS idx_point_tx_student_date ON point_transactions(student_id, created_at);

-- Seed initial 19 Element Tiers
INSERT INTO element_tiers (tier_order, tier_code, element_name, symbol, mass_number, required_points, active)
VALUES
(1, 'H-1', 'Hydrogen', 'H', 1, 10, true),
(2, 'He-4', 'Helium', 'He', 4, 40, true),
(3, 'Li-7', 'Lithium', 'Li', 7, 70, true),
(4, 'Be-9', 'Beryllium', 'Be', 9, 90, true),
(5, 'B-11', 'Boron', 'B', 11, 110, true),
(6, 'C-12', 'Carbon', 'C', 12, 120, true),
(7, 'C-14', 'Carbon', 'C', 14, 140, true),
(8, 'N-15', 'Nitrogen', 'N', 15, 150, true),
(9, 'O-16', 'Oxygen', 'O', 16, 160, true),
(10, 'F-19', 'Fluorine', 'F', 19, 190, true),
(11, 'Ne-20', 'Neon', 'Ne', 20, 200, true),
(12, 'Na-23', 'Sodium', 'Na', 23, 230, true),
(13, 'Mg-24', 'Magnesium', 'Mg', 24, 240, true),
(14, 'Al-27', 'Aluminium', 'Al', 27, 270, true),
(15, 'Si-28', 'Silicon', 'Si', 28, 280, true),
(16, 'P-31', 'Phosphorus', 'P', 31, 310, true),
(17, 'S-32', 'Sulfur', 'S', 32, 320, true),
(18, 'Cl-35', 'Chlorine', 'Cl', 35, 350, true),
(19, 'Ar-40', 'Argon', 'Ar', 40, 400, true)
ON CONFLICT (tier_code) DO NOTHING;
