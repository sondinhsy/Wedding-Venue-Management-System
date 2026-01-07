-- Migration script to add voucher columns to bookings table
-- Run this script to add voucher support to existing database

ALTER TABLE bookings ADD COLUMN voucher_code TEXT;
ALTER TABLE bookings ADD COLUMN voucher_type TEXT;
ALTER TABLE bookings ADD COLUMN voucher_value REAL;

-- Index for faster voucher code lookups (optional)
CREATE INDEX IF NOT EXISTS idx_bookings_voucher_code ON bookings(voucher_code);

