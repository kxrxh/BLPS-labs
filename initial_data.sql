-- Initialize essential data for BLPS Labs

-- Payment Providers
INSERT INTO payment_provider (name) VALUES ('Credit Card') ON CONFLICT DO NOTHING;
INSERT INTO payment_provider (name) VALUES ('PayPal') ON CONFLICT DO NOTHING;
INSERT INTO payment_provider (name) VALUES ('Bank Transfer') ON CONFLICT DO NOTHING;

-- If you need to initialize any other data that can't be created through the API
-- Add it here 