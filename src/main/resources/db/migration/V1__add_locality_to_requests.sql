-- La solicitud pasa a tener localidad (desplegable) y barrio (texto libre).
-- El barrio conserva la columna neighborhood para no reescribir datos existentes.
ALTER TABLE requests ADD COLUMN IF NOT EXISTS locality_id BIGINT;
ALTER TABLE requests ADD COLUMN IF NOT EXISTS locality VARCHAR(120);

UPDATE requests SET locality = 'Sin localidad' WHERE locality IS NULL;
ALTER TABLE requests ALTER COLUMN locality SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_requests_locality_id ON requests (locality_id);
