-- Local-dev seed data. Loaded automatically on startup by the 'local' profile
-- (see application-local.yml -> spring.sql.init.data-locations). ddl-auto is
-- create-drop, so the tables are empty on every boot and these inserts run
-- fresh each time -- no duplicates to worry about.
--
-- NOTE: created_at is NOT NULL and is normally populated by a JPA @PrePersist
-- hook. That hook does NOT run for raw SQL inserts, so it is set explicitly here.

INSERT INTO vehicles (id, make, model, model_year, engine_code, notes, created_at) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Volkswagen', 'Golf GTI',        2008, 'BWA',      'Mk5 project car', CURRENT_TIMESTAMP),
  ('22222222-2222-2222-2222-222222222222', 'Volkswagen', 'Polo',            2012, 'CGGB',     'Daily driver',    CURRENT_TIMESTAMP),
  ('33333333-3333-3333-3333-333333333333', 'BMW',        'M3',              2005, 'S54',      'E46 track build', CURRENT_TIMESTAMP),
  ('44444444-4444-4444-4444-444444444444', 'Honda',      'Civic Type R',    2007, 'K20Z4',    'FN2',             CURRENT_TIMESTAMP),
  ('55555555-5555-5555-5555-555555555555', 'Subaru',     'Impreza WRX STI', 2006, 'EJ257',    'Hawkeye',         CURRENT_TIMESTAMP),
  ('66666666-6666-6666-6666-666666666666', 'Toyota',     'Supra',           1998, '2JZ-GTE',  'MkIV',            CURRENT_TIMESTAMP),
  ('77777777-7777-7777-7777-777777777777', 'Nissan',     'Skyline GT-R',    1999, 'RB26DETT', 'R34',             CURRENT_TIMESTAMP),
  ('88888888-8888-8888-8888-888888888888', 'Ford',       'Focus RS',        2017, 'EcoBoost', 'Mk3',             CURRENT_TIMESTAMP);

-- A couple of vehicles get modifications + dyno results so the build-summary
-- endpoint has something interesting to return.
INSERT INTO modifications (id, vehicle_id, category, name, part_number, cost, installed_at, mileage_km_at_install, created_at) VALUES
  ('a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'TUNING',  'Stage 2 remap',           NULL,            650.00,  '2023-04-10',  98000, CURRENT_TIMESTAMP),
  ('a2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'EXHAUST', 'Cat-back exhaust',        'MILTEK-VW-001',  900.00,  '2023-05-01',  99000, CURRENT_TIMESTAMP),
  ('a6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'ENGINE',  'Single turbo conversion', NULL,           8500.00,  '2022-09-15', 120000, CURRENT_TIMESTAMP);

INSERT INTO dyno_results (id, vehicle_id, power_hp, torque_nm, measured_at, notes, created_at) VALUES
  ('d1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 210, 320, '2023-05-05', 'After Stage 2',         CURRENT_TIMESTAMP),
  ('d6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 480, 600, '2022-10-01', 'Single turbo, 1.2 bar', CURRENT_TIMESTAMP);
