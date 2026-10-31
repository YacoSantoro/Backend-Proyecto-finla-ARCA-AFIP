-- Datos de ejemplo. MERGE = "inserta o actualiza", asi no se duplican al reiniciar.
-- doc_tipo: 80 CUIT, 96 DNI, 99 Sin identificar
-- condicion_iva: 1 Resp. Inscripto, 4 Exento, 5 Consumidor Final, 6 Monotributo

MERGE INTO cliente (id, razon_social, doc_tipo, doc_nro, condicion_iva, direccion) KEY(id) VALUES
 (1, 'Consumidor Final',            99, 0,           5, '-'),
 (2, 'Juan Perez',                  96, 30123456,    5, 'San Martin 123, Mendoza'),
 (3, 'Maria Gomez',                 96, 28999888,    5, 'Belgrano 456, Mendoza'),
 (4, 'Ferreteria El Tornillo SRL',  80, 30712345678, 1, 'Las Heras 789, Mendoza'),
 (5, 'Kiosco Don Pepe',             80, 20223334445, 6, 'Colon 50, Godoy Cruz'),
 (6, 'Fundacion Ayudar',            80, 30655544433, 4, 'Peru 1000, Mendoza'),
 (7, 'Supermercado La Esquina SA',  80, 30700011122, 1, 'Ruta 40 km 5, Lujan'),
 (8, 'Carlos Lopez',                96, 35111222,    5, 'Mitre 77, Guaymallen');

MERGE INTO articulo (id, codigo, descripcion, alicuota_iva) KEY(id) VALUES
 (1, 'A001', 'Martillo carpintero',       21.0),
 (2, 'A002', 'Destornillador plano',      21.0),
 (3, 'A003', 'Caja tornillos x100',       21.0),
 (4, 'A004', 'Pinza universal',           21.0),
 (5, 'A005', 'Cinta metrica 5m',          21.0),
 (6, 'A006', 'Libro de bricolaje',        10.5),
 (7, 'A007', 'Taladro percutor 600W',     21.0),
 (8, 'A008', 'Guantes de trabajo',        21.0);

MERGE INTO lista_precio (id, nombre) KEY(id) VALUES
 (1, 'Minorista'),
 (2, 'Mayorista');

-- precio = precio FINAL (IVA incluido)
MERGE INTO lista_precio_articulo (id, lista_precio_id, articulo_id, precio) KEY(id) VALUES
 (1, 1, 1, 12000), (2, 1, 2, 3500), (3, 1, 3, 4200), (4, 1, 4, 8900),
 (5, 1, 5, 5600),  (6, 1, 6, 15000),(7, 1, 7, 95000),(8, 1, 8, 2800),
 (9, 2, 1, 10000), (10,2, 2, 2900), (11,2, 3, 3500), (12,2, 4, 7400),
 (13,2, 5, 4700),  (14,2, 6, 13000),(15,2, 7, 82000),(16,2, 8, 2300);
