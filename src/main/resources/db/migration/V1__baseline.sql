-- Baseline schema.
--
-- Mirrors exactly what Hibernate's ddl-auto previously generated for PostgreSQL,
-- so `spring.jpa.hibernate.ddl-auto: validate` accepts it. Column types must stay
-- in step with the entities from here on: change an entity, add a migration.
--
-- Existing databases (already built by ddl-auto) are stamped as being at this
-- version via `spring.flyway.baseline-on-migrate`, so this script does not re-run
-- against them. An empty database gets it applied normally.

create table users (
    id            uuid         not null,
    name          varchar(255),
    email         varchar(255) not null unique,
    username      varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(32)  not null check (role in ('USER', 'ADMIN')),
    primary key (id)
);

create table vehicles (
    id          uuid        not null,
    owner_id    uuid        not null,
    make        varchar(255) not null,
    model       varchar(255) not null,
    model_year  integer     not null,
    engine_code varchar(255) not null,
    status      varchar(32) not null check (status in ('PROJECT', 'DAILY', 'SOLD')),
    notes       text,
    created_at  timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_vehicles_owner foreign key (owner_id) references users
);

create table modifications (
    id                    uuid           not null,
    vehicle_id            uuid           not null,
    category              varchar(32)    not null check (category in
                              ('ENGINE', 'EXHAUST', 'INTAKE', 'SUSPENSION',
                               'BRAKES', 'TUNING', 'COSMETIC', 'OTHER')),
    name                  varchar(255)   not null,
    part_number           varchar(255),
    cost                  numeric(12, 2) not null,
    installed_at          date           not null,
    mileage_km_at_install integer        not null,
    created_at            timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_modifications_vehicle foreign key (vehicle_id) references vehicles
);

create table dyno_results (
    id          uuid    not null,
    vehicle_id  uuid    not null,
    power_hp    integer not null,
    torque_nm   integer not null,
    measured_at date    not null,
    notes       text,
    created_at  timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_dyno_results_vehicle foreign key (vehicle_id) references vehicles
);
