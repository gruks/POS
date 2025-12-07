-- Insert sample restaurant tables
-- Run this if tables are not loading in the Billing view

INSERT INTO restaurant_tables (name, capacity, status)
VALUES 
    ('Table 1', 4, 'Available'),
    ('Table 2', 4, 'Available'),
    ('Table 3', 2, 'Available'),
    ('Table 4', 2, 'Available'),
    ('Table 5', 6, 'Available'),
    ('Table 6', 6, 'Available'),
    ('Table 7', 4, 'Available'),
    ('Table 8', 4, 'Available'),
    ('Table 9', 8, 'Available'),
    ('Table 10', 2, 'Available')
ON CONFLICT (name) DO NOTHING;

-- Verify tables were inserted
SELECT COUNT(*) as table_count FROM restaurant_tables;
SELECT * FROM restaurant_tables ORDER BY name;
