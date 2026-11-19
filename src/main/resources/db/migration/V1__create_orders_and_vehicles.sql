CREATE TABLE orders (
                        order_id       VARCHAR(64) PRIMARY KEY,
                        latitude       DOUBLE PRECISION NOT NULL,
                        longitude      DOUBLE PRECISION NOT NULL,
                        address        TEXT NOT NULL,
                        package_weight DOUBLE PRECISION NOT NULL CHECK (package_weight > 0),
                        priority       VARCHAR(16) NOT NULL CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE TABLE vehicles (
                          vehicle_id        VARCHAR(64) PRIMARY KEY,
                          capacity          DOUBLE PRECISION NOT NULL CHECK (capacity > 0),
                          current_latitude  DOUBLE PRECISION NOT NULL,
                          current_longitude DOUBLE PRECISION NOT NULL,
                          current_address   TEXT NOT NULL
);