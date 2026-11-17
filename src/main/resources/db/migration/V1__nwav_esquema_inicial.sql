-- Esquema inicial de NWAV Editor PDF.
-- Los PDF nunca se guardan aqui: solo rutas y datos propios de la aplicacion.

CREATE TABLE nwav_archivo_reciente (
    nwav_id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nwav_ruta            VARCHAR(2048) NOT NULL UNIQUE,
    nwav_nombre          VARCHAR(255)  NOT NULL,
    nwav_ultima_apertura TIMESTAMP     NOT NULL,
    nwav_ultima_pagina   INTEGER       NOT NULL DEFAULT 0,
    nwav_zoom            REAL          NOT NULL DEFAULT 1.0,
    nwav_modo_zoom       VARCHAR(30)   NOT NULL DEFAULT 'NWAV_PORCENTAJE'
);
CREATE INDEX nwav_idx_reciente_apertura ON nwav_archivo_reciente (nwav_ultima_apertura DESC);

CREATE TABLE nwav_configuracion (
    nwav_clave VARCHAR(100) PRIMARY KEY,
    nwav_valor VARCHAR(2048)
);

CREATE TABLE nwav_firma (
    nwav_id             INTEGER PRIMARY KEY AUTOINCREMENT,
    nwav_nombre         VARCHAR(120) NOT NULL,
    nwav_imagen         BLOB         NOT NULL,
    nwav_fecha_creacion TIMESTAMP    NOT NULL
);

CREATE TABLE nwav_marca_agua (
    nwav_id             INTEGER PRIMARY KEY AUTOINCREMENT,
    nwav_nombre         VARCHAR(120) NOT NULL,
    nwav_tipo           VARCHAR(20)  NOT NULL,
    nwav_texto          VARCHAR(500),
    nwav_imagen         BLOB,
    nwav_fuente         VARCHAR(60),
    nwav_tamano_fuente  REAL,
    nwav_color          VARCHAR(9),
    nwav_opacidad       REAL         NOT NULL DEFAULT 0.3,
    nwav_rotacion       REAL         NOT NULL DEFAULT 45,
    nwav_escala         REAL         NOT NULL DEFAULT 1.0,
    nwav_fecha_creacion TIMESTAMP    NOT NULL
);

CREATE TABLE nwav_historial_operacion (
    nwav_id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nwav_tipo            VARCHAR(40)   NOT NULL,
    nwav_archivo_origen  VARCHAR(4096),
    nwav_archivo_destino VARCHAR(2048),
    nwav_fecha           TIMESTAMP     NOT NULL,
    nwav_resultado       VARCHAR(20)   NOT NULL,
    nwav_detalle         VARCHAR(2048)
);
CREATE INDEX nwav_idx_historial_fecha ON nwav_historial_operacion (nwav_fecha DESC);
