-- Posicion de la marca de agua (o mosaico) en las plantillas guardadas.
ALTER TABLE nwav_marca_agua ADD COLUMN nwav_posicion VARCHAR(30) NOT NULL DEFAULT 'NWAV_CENTRO';
