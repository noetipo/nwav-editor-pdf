-- Ultimas opciones usadas en cada herramienta (clave-valor por herramienta).
CREATE TABLE nwav_opcion_herramienta (
    nwav_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nwav_herramienta VARCHAR(60)   NOT NULL,
    nwav_clave       VARCHAR(80)   NOT NULL,
    nwav_valor       VARCHAR(2048),
    nwav_actualizado TIMESTAMP     NOT NULL,
    CONSTRAINT nwav_uq_opcion_herramienta UNIQUE (nwav_herramienta, nwav_clave)
);
