ALTER TABLE admins
  ADD COLUMN pending_otp_secret TEXT NULL,
  ADD COLUMN otp_secret TEXT NULL;
