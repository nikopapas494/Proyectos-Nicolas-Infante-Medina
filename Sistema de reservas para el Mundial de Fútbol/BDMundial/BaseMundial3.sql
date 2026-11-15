-- ============================================================
--  SGEM  –  Sistema de Gestión de Experiencias Mundialistas
--  Copa Mundial FIFA 2026
--  Script corregido – Tercera Entrega
-- ============================================================

-- ------------------------------------------------------------
--  1. DROP (orden inverso de dependencias)
-- ------------------------------------------------------------
DROP TABLE novedad           CASCADE CONSTRAINTS;
DROP TABLE pago              CASCADE CONSTRAINTS;
DROP TABLE inscxservicio     CASCADE CONSTRAINTS;
DROP TABLE inscripcion       CASCADE CONSTRAINTS;
DROP TABLE srvxexperiencia   CASCADE CONSTRAINTS;
DROP TABLE srvximpuesto      CASCADE CONSTRAINTS;
DROP TABLE guiaxexperiencia  CASCADE CONSTRAINTS;
DROP TABLE servicio          CASCADE CONSTRAINTS;
DROP TABLE impuesto          CASCADE CONSTRAINTS;
DROP TABLE experiencia       CASCADE CONSTRAINTS;
DROP TABLE partido           CASCADE CONSTRAINTS;
DROP TABLE estadio           CASCADE CONSTRAINTS;
DROP TABLE guia              CASCADE CONSTRAINTS;
DROP TABLE cliente           CASCADE CONSTRAINTS;

-- ------------------------------------------------------------
--  2. CREATE TABLES
-- ------------------------------------------------------------

CREATE TABLE estadio (
    id_estadio  NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR2(150)  NOT NULL,
    ciudad      VARCHAR2(100)  NOT NULL,
    pais        VARCHAR2(50)   NOT NULL,
    capacidad   NUMBER         NOT NULL
);

CREATE TABLE partido (
    id_partido       NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipo_local     VARCHAR2(100)  NOT NULL,
    equipo_visitante VARCHAR2(100)  NOT NULL,
    fecha            DATE           NOT NULL,
    fase             VARCHAR2(50)   NOT NULL
                     CONSTRAINT chk_fase CHECK (fase IN ('Grupos','Octavos','Cuartos','Semifinal','Final')),
    id_estadio       NUMBER         NOT NULL,
    CONSTRAINT fk_partido_estadio FOREIGN KEY (id_estadio) REFERENCES estadio(id_estadio)
);

CREATE TABLE guia (
    id_guia          NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_documento   VARCHAR2(20)   NOT NULL,
    numero_documento VARCHAR2(30)   NOT NULL,
    nombre           VARCHAR2(150)  NOT NULL,
    nivel            VARCHAR2(20)   NOT NULL
                     CONSTRAINT chk_nivel_guia CHECK (nivel IN ('BÁSICO','INTERMEDIO','EXPERTO')),
    valor_hora       NUMBER(10,2)   NOT NULL,
    CONSTRAINT uq_guia_doc UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE cliente (
    id_cliente       NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_completo  VARCHAR2(150)  NOT NULL,
    tipo_documento   VARCHAR2(20)   NOT NULL,
    numero_documento VARCHAR2(30)   NOT NULL,
    correo           VARCHAR2(100)  NOT NULL,
    telefono         VARCHAR2(20)   NOT NULL,
    CONSTRAINT uq_cliente_doc    UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uq_cliente_correo UNIQUE (correo)
);

CREATE TABLE experiencia (
    id_experiencia    NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descripcion       VARCHAR2(500)  NOT NULL,
    estado            VARCHAR2(20)   NOT NULL
                      CONSTRAINT chk_estado_exp CHECK (estado IN ('disponible','agotada','cancelada')),
    precio_base       NUMBER(10,2)   NOT NULL,
    cupos_disponibles NUMBER         NOT NULL,
    fecha_hora        TIMESTAMP      NOT NULL,
    id_estadio        NUMBER         NOT NULL,
    id_partido        NUMBER         NOT NULL,
    id_guia_principal NUMBER         NOT NULL,
    CONSTRAINT fk_exp_estadio FOREIGN KEY (id_estadio)        REFERENCES estadio(id_estadio),
    CONSTRAINT fk_exp_partido FOREIGN KEY (id_partido)        REFERENCES partido(id_partido),
    CONSTRAINT fk_exp_guia    FOREIGN KEY (id_guia_principal) REFERENCES guia(id_guia),
    CONSTRAINT uq_exp_estadio_fecha UNIQUE (id_estadio, fecha_hora)
);

CREATE TABLE guiaxexperiencia (
    id_guia        NUMBER NOT NULL,
    id_experiencia NUMBER NOT NULL,
    CONSTRAINT pk_guiaxexp PRIMARY KEY (id_guia, id_experiencia),
    CONSTRAINT fk_gxe_guia FOREIGN KEY (id_guia)        REFERENCES guia(id_guia),
    CONSTRAINT fk_gxe_exp  FOREIGN KEY (id_experiencia) REFERENCES experiencia(id_experiencia)
);

CREATE TABLE servicio (
    id_servicio     NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR2(100)  NOT NULL,
    descripcion     VARCHAR2(300)  NOT NULL,
    costo_adicional NUMBER(10,2)   NOT NULL
);

CREATE TABLE srvxexperiencia (
    id_servicio    NUMBER NOT NULL,
    id_experiencia NUMBER NOT NULL,
    CONSTRAINT pk_srvxexp PRIMARY KEY (id_servicio, id_experiencia),
    CONSTRAINT fk_sxe_srv FOREIGN KEY (id_servicio)    REFERENCES servicio(id_servicio),
    CONSTRAINT fk_sxe_exp FOREIGN KEY (id_experiencia) REFERENCES experiencia(id_experiencia)
);

CREATE TABLE impuesto (
    id_impuesto NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR2(100)  NOT NULL,
    tipo        VARCHAR2(50)   NOT NULL
                CONSTRAINT chk_tipo_impuesto CHECK (tipo IN ('IVA','Turístico','Local','Otros')),
    porcentaje  NUMBER(5,2)    NOT NULL
);

CREATE TABLE srvximpuesto (
    id_servicio NUMBER NOT NULL,
    id_impuesto NUMBER NOT NULL,
    CONSTRAINT pk_srvximp PRIMARY KEY (id_servicio, id_impuesto),
    CONSTRAINT fk_sxi_srv FOREIGN KEY (id_servicio) REFERENCES servicio(id_servicio),
    CONSTRAINT fk_sxi_imp FOREIGN KEY (id_impuesto) REFERENCES impuesto(id_impuesto)
);

CREATE TABLE inscripcion (
    id_inscripcion    NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    num_reserva       VARCHAR2(20)   NOT NULL,
    fecha_inscripcion DATE           NOT NULL,
    costo_total       NUMBER(10,2)   NOT NULL,
    saldo_pendiente   NUMBER(10,2)   NOT NULL,
    id_experiencia    NUMBER         NOT NULL,
    id_cliente        NUMBER         NOT NULL,
    CONSTRAINT fk_ins_exp     FOREIGN KEY (id_experiencia) REFERENCES experiencia(id_experiencia),
    CONSTRAINT fk_ins_cli     FOREIGN KEY (id_cliente)     REFERENCES cliente(id_cliente),
    CONSTRAINT uq_ins_reserva UNIQUE (num_reserva),
    -- CORRECCIÓN: se eliminó UNIQUE(id_cliente, id_experiencia) porque un cliente
    -- puede tener varias inscripciones en experiencias distintas; el control de
    -- duplicados real se maneja en la capa de servicio Java.
    CONSTRAINT chk_saldo      CHECK (saldo_pendiente >= 0)
);

CREATE TABLE inscxservicio (
    id_inscripcion  NUMBER NOT NULL,
    id_servicio     NUMBER NOT NULL,
    cantidad        NUMBER NOT NULL,
    precio_unitario NUMBER(10,2) NOT NULL,
    impuesto_aplicado NUMBER(10,2) DEFAULT 0 NOT NULL,
    CONSTRAINT pk_inscxsrv  PRIMARY KEY (id_inscripcion, id_servicio),
    CONSTRAINT fk_ixs_ins   FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion),
    CONSTRAINT fk_ixs_srv   FOREIGN KEY (id_servicio)    REFERENCES servicio(id_servicio),
    CONSTRAINT chk_cantidad CHECK (cantidad > 0)
);

CREATE TABLE pago (
    id_pago        NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_inscripcion NUMBER         NOT NULL,
    referencia     VARCHAR2(100)  NOT NULL,
    valor_pagado   NUMBER(10,2)   NOT NULL,
    fecha_pago     DATE           NOT NULL,
    tipo_pago      VARCHAR2(50)   NOT NULL
                   CONSTRAINT chk_tipo_pago CHECK (tipo_pago IN ('crédito','débito','efectivo','billetera digital')),
    CONSTRAINT fk_pago_ins FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion),
    CONSTRAINT uq_pago_ref UNIQUE (referencia)
);

-- NUEVA: tabla de novedades/historial (alimentada por trigger de HU2)
CREATE TABLE novedad (
    id_novedad     NUMBER        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_inscripcion NUMBER        NOT NULL,
    descripcion    VARCHAR2(500) NOT NULL,
    fecha_novedad  DATE          DEFAULT SYSDATE NOT NULL,
    tipo           VARCHAR2(50)  NOT NULL
                   CONSTRAINT chk_tipo_novedad
                   CHECK (tipo IN ('MODIFICACION_SERVICIO','PAGO','OBSERVACION','OTRO')),
    registrado_por VARCHAR2(100) DEFAULT 'SISTEMA' NOT NULL,
    CONSTRAINT fk_nov_ins FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion)
);

-- ------------------------------------------------------------
--  3. TRIGGERS
-- ------------------------------------------------------------

-- TRIGGER 1: valida cupos disponibles antes de insertar una inscripción
--            y descuenta el cupo si hay disponibilidad
CREATE OR REPLACE TRIGGER trg_validar_cupos
BEFORE INSERT ON inscripcion
FOR EACH ROW
DECLARE
    v_cupos     NUMBER;
    v_estado    VARCHAR2(20);
BEGIN
    SELECT cupos_disponibles, estado
      INTO v_cupos, v_estado
      FROM experiencia
     WHERE id_experiencia = :NEW.id_experiencia;

    IF v_estado != 'disponible' THEN
        RAISE_APPLICATION_ERROR(-20001,
            'La experiencia no está disponible (estado: ' || v_estado || ').');
    END IF;

    IF v_cupos <= 0 THEN
        RAISE_APPLICATION_ERROR(-20002,
            'No hay cupos disponibles para esta experiencia.');
    END IF;

    -- Descontar cupo y marcar como agotada si llega a 0
    UPDATE experiencia
       SET cupos_disponibles = cupos_disponibles - 1,
           estado = CASE WHEN cupos_disponibles - 1 = 0 THEN 'agotada' ELSE estado END
     WHERE id_experiencia = :NEW.id_experiencia;
END;
/

-- TRIGGER 2: al registrar un pago, actualiza automáticamente el saldo pendiente
--            y registra la novedad en el historial
CREATE OR REPLACE TRIGGER trg_actualizar_saldo
AFTER INSERT ON pago
FOR EACH ROW
DECLARE
    v_saldo_actual NUMBER;
    v_nuevo_saldo  NUMBER;
BEGIN
    SELECT saldo_pendiente INTO v_saldo_actual
      FROM inscripcion
     WHERE id_inscripcion = :NEW.id_inscripcion;

    v_nuevo_saldo := v_saldo_actual - :NEW.valor_pagado;

    IF v_nuevo_saldo < 0 THEN
        RAISE_APPLICATION_ERROR(-20003,
            'El valor del pago supera el saldo pendiente.');
    END IF;

    UPDATE inscripcion
       SET saldo_pendiente = v_nuevo_saldo
     WHERE id_inscripcion = :NEW.id_inscripcion;

    INSERT INTO novedad (id_inscripcion, descripcion, tipo, registrado_por)
    VALUES (:NEW.id_inscripcion,
            'Pago registrado. Referencia: ' || :NEW.referencia ||
            '. Valor: ' || :NEW.valor_pagado ||
            '. Nuevo saldo pendiente: ' || v_nuevo_saldo,
            'PAGO', 'SISTEMA');
END;
/

-- ------------------------------------------------------------
--  4. INSERTS
-- ------------------------------------------------------------

-- ESTADIOS
INSERT INTO estadio (nombre, ciudad, pais, capacidad) VALUES ('Estadio Azteca',  'Ciudad de México','México',        87523);
INSERT INTO estadio (nombre, ciudad, pais, capacidad) VALUES ('Estadio Akron',   'Guadalajara',     'México',        49850);
INSERT INTO estadio (nombre, ciudad, pais, capacidad) VALUES ('MetLife Stadium', 'Nueva York',      'Estados Unidos',82500);
INSERT INTO estadio (nombre, ciudad, pais, capacidad) VALUES ('ATT Stadium',     'Dallas',          'Estados Unidos',80000);
INSERT INTO estadio (nombre, ciudad, pais, capacidad) VALUES ('BC Place',        'Vancouver',       'Canadá',        54500);
INSERT INTO estadio (nombre, ciudad, pais, capacidad) VALUES ('BMO Field',       'Toronto',         'Canadá',        45736);

-- PARTIDOS
INSERT INTO partido (equipo_local, equipo_visitante, fecha, fase, id_estadio) VALUES ('Colombia', 'Portugal',  DATE '2026-06-15','Grupos',   1);
INSERT INTO partido (equipo_local, equipo_visitante, fecha, fase, id_estadio) VALUES ('México',   'Argentina', DATE '2026-06-18','Grupos',   2);
INSERT INTO partido (equipo_local, equipo_visitante, fecha, fase, id_estadio) VALUES ('Brasil',   'Francia',   DATE '2026-06-20','Grupos',   3);
INSERT INTO partido (equipo_local, equipo_visitante, fecha, fase, id_estadio) VALUES ('Alemania', 'España',    DATE '2026-07-02','Octavos',  4);
INSERT INTO partido (equipo_local, equipo_visitante, fecha, fase, id_estadio) VALUES ('Canadá',   'Marruecos', DATE '2026-06-22','Grupos',   5);
INSERT INTO partido (equipo_local, equipo_visitante, fecha, fase, id_estadio) VALUES ('Inglaterra','Japón',    DATE '2026-07-10','Cuartos',  6);

-- GUÍAS
INSERT INTO guia (tipo_documento, numero_documento, nombre, nivel, valor_hora) VALUES ('CC', '1020304050','Andrés Ramírez',  'EXPERTO',    85000);
INSERT INTO guia (tipo_documento, numero_documento, nombre, nivel, valor_hora) VALUES ('CC', '2030405060','Valentina Torres','INTERMEDIO', 60000);
INSERT INTO guia (tipo_documento, numero_documento, nombre, nivel, valor_hora) VALUES ('CE', '3040506070','Carlos Méndez',   'BÁSICO',     40000);
INSERT INTO guia (tipo_documento, numero_documento, nombre, nivel, valor_hora) VALUES ('CC', '4050607080','Laura Pineda',    'EXPERTO',    85000);
INSERT INTO guia (tipo_documento, numero_documento, nombre, nivel, valor_hora) VALUES ('PA', 'PA9988776', 'James Wilson',    'INTERMEDIO', 60000);
INSERT INTO guia (tipo_documento, numero_documento, nombre, nivel, valor_hora) VALUES ('CC', '5060708090','Sofía Herrera',   'BÁSICO',     40000);

-- CLIENTES
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Juan Carlos Pérez','CC', '80112233',  'jcperez@mail.com',   '3001234567');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('María López',      'CC', '52334455',  'mlopez@mail.com',    '3119876543');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Roberto Gómez',    'CE', '91556677',  'rgomez@mail.com',    '3204567890');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Ana Martínez',     'PA', 'PA1234567', 'amartinez@mail.com', '3056781234');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Felipe Cardona',   'CC', '10778899',  'fcardona@mail.com',  '3143456789');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Isabella Cruz',    'CC', '10990011',  'icruz@mail.com',     '3165678901');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Santiago Morales', 'CC', '11223344',  'smorales@mail.com',  '3201112233');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Camila Rios',      'CC', '22334455',  'crios@mail.com',     '3152223344');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Diego Vargas',     'CE', '33445566',  'dvargas@mail.com',   '3003334455');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Luciana Peña',     'PA', 'PA5566778', 'lpena@mail.com',     '3174445566');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Tomás Aguirre',    'CC', '44556677',  'taguirre@mail.com',  '3125556677');
INSERT INTO cliente (nombre_completo, tipo_documento, numero_documento, correo, telefono) VALUES ('Valeria Ortiz',    'CC', '55667788',  'vortiz@mail.com',    '3056667788');

-- EXPERIENCIAS
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Experiencia VIP Colombia vs Portugal en Azteca',   'disponible', 2500000, 30, TIMESTAMP '2026-06-15 14:00:00', 1, 1, 1);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete fanático México vs Argentina en Akron',    'disponible', 1800000, 30, TIMESTAMP '2026-06-18 18:00:00', 2, 2, 2);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Tour premium Brasil vs Francia en MetLife',        'disponible', 3200000, 25, TIMESTAMP '2026-06-20 20:00:00', 3, 3, 3);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Experiencia Octavos Alemania vs España en ATT',    'agotada',    2800000,  0, TIMESTAMP '2026-07-02 16:00:00', 4, 4, 4);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete Canadá vs Marruecos en BC Place',          'disponible', 1500000, 25, TIMESTAMP '2026-06-22 15:00:00', 5, 5, 5);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Experiencia Cuartos Inglaterra vs Japón BMO',      'cancelada',  2200000, 10, TIMESTAMP '2026-07-10 19:00:00', 6, 6, 6);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete estándar Colombia vs Portugal en Azteca',  'disponible', 1200000, 50, TIMESTAMP '2026-06-15 10:00:00', 1, 1, 2);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete económico Colombia vs Portugal Azteca',    'agotada',     800000,  0, TIMESTAMP '2026-06-15 08:00:00', 1, 1, 3);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Experiencia Premium México vs Argentina Akron',    'disponible', 2900000, 20, TIMESTAMP '2026-06-18 10:00:00', 2, 2, 4);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete básico Brasil vs Francia MetLife',         'disponible', 1100000, 40, TIMESTAMP '2026-06-20 10:00:00', 3, 3, 5);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete VIP Canadá vs Marruecos BC Place',         'disponible', 2100000, 20, TIMESTAMP '2026-06-22 09:00:00', 5, 5, 1);
INSERT INTO experiencia (descripcion, estado, precio_base, cupos_disponibles, fecha_hora, id_estadio, id_partido, id_guia_principal)
    VALUES ('Paquete fanático Alemania vs España ATT',          'disponible', 1900000, 18, TIMESTAMP '2026-07-02 10:00:00', 4, 4, 6);

-- GUÍAS SUPLENTES
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (2,  1);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (3,  1);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (1,  2);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (4,  3);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (6,  5);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (5,  7);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (6,  7);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (2,  9);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (3, 10);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (2, 11);
INSERT INTO guiaxexperiencia (id_guia, id_experiencia) VALUES (4, 12);

-- SERVICIOS
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Transporte aeropuerto', 'Traslado ida y vuelta aeropuerto-estadio en van privada',   150000);
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Alimentación completa', 'Desayuno, almuerzo y cena gourmet durante la experiencia',   200000);
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Acceso VIP lounge',     'Ingreso a sala VIP con open bar y vista al campo',           350000);
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Souvenir oficial',      'Kit con camiseta, bufanda y balón oficial FIFA',             180000);
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Traducción simultánea', 'Guía traductor en inglés, francés o portugués',             120000);
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Seguro de viaje',       'Cobertura médica y de equipaje durante toda la experiencia', 80000);
INSERT INTO servicio (nombre, descripcion, costo_adicional) VALUES ('Alimentación premium',  'Menú gourmet premium con maridaje de vinos incluido',        300000);

-- SERVICIOS DISPONIBLES POR EXPERIENCIA
-- Experiencia 1: VIP Colombia vs Portugal
INSERT INTO srvxexperiencia VALUES (1, 1);
INSERT INTO srvxexperiencia VALUES (2, 1);
INSERT INTO srvxexperiencia VALUES (3, 1);
INSERT INTO srvxexperiencia VALUES (4, 1);
INSERT INTO srvxexperiencia VALUES (5, 1);
INSERT INTO srvxexperiencia VALUES (7, 1);
-- Experiencia 2: Fanático México vs Argentina
INSERT INTO srvxexperiencia VALUES (1, 2);
INSERT INTO srvxexperiencia VALUES (2, 2);
INSERT INTO srvxexperiencia VALUES (4, 2);
INSERT INTO srvxexperiencia VALUES (5, 2);
-- Experiencia 3: Premium Brasil vs Francia
INSERT INTO srvxexperiencia VALUES (1, 3);
INSERT INTO srvxexperiencia VALUES (3, 3);
INSERT INTO srvxexperiencia VALUES (6, 3);
INSERT INTO srvxexperiencia VALUES (7, 3);
-- Experiencia 5: Canadá vs Marruecos
INSERT INTO srvxexperiencia VALUES (1, 5);
INSERT INTO srvxexperiencia VALUES (4, 5);
INSERT INTO srvxexperiencia VALUES (6, 5);
-- Experiencia 7: Estándar Colombia vs Portugal
INSERT INTO srvxexperiencia VALUES (1, 7);
INSERT INTO srvxexperiencia VALUES (4, 7);
INSERT INTO srvxexperiencia VALUES (6, 7);
-- Experiencia 8: Económico Colombia vs Portugal
INSERT INTO srvxexperiencia VALUES (1, 8);
-- Experiencia 9: Premium México vs Argentina
INSERT INTO srvxexperiencia VALUES (1, 9);
INSERT INTO srvxexperiencia VALUES (2, 9);
INSERT INTO srvxexperiencia VALUES (3, 9);
INSERT INTO srvxexperiencia VALUES (5, 9);
INSERT INTO srvxexperiencia VALUES (7, 9);
-- Experiencia 10: Básico Brasil vs Francia
INSERT INTO srvxexperiencia VALUES (1, 10);
INSERT INTO srvxexperiencia VALUES (2, 10);
-- Experiencia 11: VIP Canadá vs Marruecos
INSERT INTO srvxexperiencia VALUES (1, 11);
INSERT INTO srvxexperiencia VALUES (3, 11);
INSERT INTO srvxexperiencia VALUES (4, 11);
INSERT INTO srvxexperiencia VALUES (6, 11);
INSERT INTO srvxexperiencia VALUES (7, 11);
-- Experiencia 12: Fanático Alemania vs España
INSERT INTO srvxexperiencia VALUES (1, 12);
INSERT INTO srvxexperiencia VALUES (2, 12);
INSERT INTO srvxexperiencia VALUES (5, 12);

-- IMPUESTOS
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('IVA México',          'IVA',       16.00);
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('IVA Estados Unidos',  'IVA',        8.00);
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('IVA Canadá',          'IVA',       13.00);
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('Impuesto Turístico',  'Turístico',  5.00);
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('Impuesto Ciudad Mex', 'Local',      3.00);
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('Impuesto Ciudad NY',  'Local',      4.50);
INSERT INTO impuesto (nombre, tipo, porcentaje) VALUES ('Tasa FIFA',           'Otros',      2.00);

-- IMPUESTOS POR SERVICIO
INSERT INTO srvximpuesto VALUES (1, 1);  -- Transporte → IVA México
INSERT INTO srvximpuesto VALUES (1, 4);  -- Transporte → Turístico
INSERT INTO srvximpuesto VALUES (2, 1);  -- Alimentación → IVA México
INSERT INTO srvximpuesto VALUES (2, 5);  -- Alimentación → Local Mex
INSERT INTO srvximpuesto VALUES (3, 2);  -- VIP lounge  → IVA EEUU
INSERT INTO srvximpuesto VALUES (3, 6);  -- VIP lounge  → Local NY
INSERT INTO srvximpuesto VALUES (3, 7);  -- VIP lounge  → Tasa FIFA
INSERT INTO srvximpuesto VALUES (4, 1);  -- Souvenir    → IVA México
INSERT INTO srvximpuesto VALUES (4, 7);  -- Souvenir    → Tasa FIFA
INSERT INTO srvximpuesto VALUES (5, 4);  -- Traducción  → Turístico
INSERT INTO srvximpuesto VALUES (6, 4);  -- Seguro      → Turístico
INSERT INTO srvximpuesto VALUES (7, 1);  -- Alim. prem. → IVA México
INSERT INTO srvximpuesto VALUES (7, 5);  -- Alim. prem. → Local Mex

-- ============================================================
--  INSCRIPCIONES
--  CORRECCIÓN: todos los inserts incluyen num_reserva y saldo_pendiente.
--  Algunas inscripciones quedan con saldo > 0 para que la consulta
--  "clientes con pagos pendientes" retorne resultados reales.
--
--  Formato num_reserva: RES-YYYY-NNN
--  saldo_pendiente inicial = costo_total (se reduce con cada pago via trigger)
--
--  NOTA: el trigger trg_validar_cupos descuenta cupos en cada INSERT,
--        por lo que los cupos_disponibles de experiencia quedarán
--        correctamente actualizados tras todos los inserts.
-- ============================================================

-- Desactivamos el trigger de cupos durante la carga masiva de datos
-- para no depender del orden de inserts y re-calcular al final.
-- En producción el trigger siempre debe estar activo.
ALTER TRIGGER trg_validar_cupos DISABLE;

INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-001', DATE '2026-05-01', 3080000,       0, 1,  1);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-002', DATE '2026-05-02', 2150000,       0, 2,  2);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-003', DATE '2026-05-03', 3870000,       0, 3,  3);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-004', DATE '2026-05-04', 2780000,       0, 1,  4);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-005', DATE '2026-05-05', 1980000,       0, 5,  5);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-006', DATE '2026-05-06', 2620000,       0, 2,  6);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-007', DATE '2026-05-07', 1650000,       0, 7,  1);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-008', DATE '2026-05-08', 2150000,       0, 9,  1);  -- CORRECCIÓN: exp 9 (no exp 2, duplicado)
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-009', DATE '2026-05-07', 1380000,       0,10,  2);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-010', DATE '2026-05-08', 2450000,       0, 9,  3);  -- CORRECCIÓN: cliente 3 (no duplicar exp)
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-011', DATE '2026-05-09', 2460000, 1000000,11,  4);  -- saldo pendiente: 1.000.000
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-012', DATE '2026-05-10', 2250000,  750000,12,  5);  -- saldo pendiente: 750.000
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-013', DATE '2026-05-09', 1350000,       0, 7,  6);  -- CORRECCIÓN: cliente 6 (era cliente 5, duplicado exp 5)
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-014', DATE '2026-05-10', 1520000,  500000,10,  7);  -- saldo pendiente: 500.000
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-015', DATE '2026-05-10', 3100000,       0, 9,  8);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-016', DATE '2026-05-11', 2700000,       0, 1,  9);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-017', DATE '2026-05-11', 1450000,  450000, 7, 10);  -- saldo pendiente: 450.000
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-018', DATE '2026-05-12', 2050000,       0,12, 11);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-019', DATE '2026-05-12', 1800000,       0, 5, 12);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-020', DATE '2026-05-13', 1250000,  300000,10, 12);  -- saldo pendiente: 300.000
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-021', DATE '2026-05-13', 3350000,       0, 3,  7);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-022', DATE '2026-05-14', 1980000,  980000,11,  8);  -- saldo pendiente: 980.000
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-023', DATE '2026-05-14', 2600000,       0, 9, 10);
INSERT INTO inscripcion (num_reserva, fecha_inscripcion, costo_total, saldo_pendiente, id_experiencia, id_cliente)
    VALUES ('RES-2026-024', DATE '2026-05-15', 1350000,  350000, 5,  9);  -- saldo pendiente: 350.000

-- Reactivar trigger de cupos
ALTER TRIGGER trg_validar_cupos ENABLE;

-- Recalcular cupos_disponibles manualmente después de la carga masiva
UPDATE experiencia e
   SET cupos_disponibles = cupos_disponibles - (
       SELECT COUNT(*) FROM inscripcion i WHERE i.id_experiencia = e.id_experiencia
   )
 WHERE estado = 'disponible';

-- Marcar como agotadas las que quedaron en 0
UPDATE experiencia SET estado = 'agotada' WHERE cupos_disponibles <= 0 AND estado = 'disponible';

-- ============================================================
--  SERVICIOS POR INSCRIPCIÓN
--  CORRECCIÓN: solo se insertan servicios que existen en srvxexperiencia
--  para la experiencia de cada inscripción.
--  Se agregan precio_unitario e impuesto_aplicado.
-- ============================================================

-- Inscripcion 1 (exp 1): servicios disponibles: 1,2,3,4,5,7
INSERT INTO inscxservicio VALUES (1, 1, 1, 150000, 31500);  -- Transporte
INSERT INTO inscxservicio VALUES (1, 2, 2, 200000, 38000);  -- Alimentación x2

-- Inscripcion 2 (exp 2): servicios disponibles: 1,2,4,5
INSERT INTO inscxservicio VALUES (2, 1, 1, 150000, 31500);  -- Transporte

-- Inscripcion 3 (exp 3): servicios disponibles: 1,3,6,7
INSERT INTO inscxservicio VALUES (3, 1, 1, 150000, 31500);  -- Transporte
INSERT INTO inscxservicio VALUES (3, 3, 1, 350000, 52325);  -- VIP lounge

-- Inscripcion 4 (exp 1): servicios disponibles: 1,2,3,4,5,7
INSERT INTO inscxservicio VALUES (4, 4, 2, 180000, 18720);  -- Souvenir x2

-- Inscripcion 5 (exp 5): servicios disponibles: 1,4,6
INSERT INTO inscxservicio VALUES (5, 1, 1, 150000, 31500);  -- Transporte

-- Inscripcion 6 (exp 2): servicios disponibles: 1,2,4,5
INSERT INTO inscxservicio VALUES (6, 5, 1, 120000,  6000);  -- Traducción
INSERT INTO inscxservicio VALUES (6, 4, 1, 180000, 18720);  -- Souvenir

-- Inscripcion 7 (exp 7): servicios disponibles: 1,4,6
INSERT INTO inscxservicio VALUES (7, 1, 1, 150000, 31500);  -- Transporte
INSERT INTO inscxservicio VALUES (7, 4, 1, 180000, 18720);  -- Souvenir

-- Inscripcion 8 (exp 9): servicios disponibles: 1,2,3,5,7
INSERT INTO inscxservicio VALUES (8, 1, 1, 150000, 31500);  -- Transporte
INSERT INTO inscxservicio VALUES (8, 5, 1, 120000,  6000);  -- Traducción

-- Inscripcion 9 (exp 10): servicios disponibles: 1,2
INSERT INTO inscxservicio VALUES (9, 1, 1, 150000, 31500);  -- Transporte
INSERT INTO inscxservicio VALUES (9, 2, 1, 200000, 38000);  -- Alimentación

-- Inscripcion 10 (exp 9): servicios disponibles: 1,2,3,5,7
INSERT INTO inscxservicio VALUES (10, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (10, 3, 1, 350000, 52325); -- VIP lounge
INSERT INTO inscxservicio VALUES (10, 2, 1, 200000, 38000); -- Alimentación

-- Inscripcion 11 (exp 11): servicios disponibles: 1,3,4,6,7
INSERT INTO inscxservicio VALUES (11, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (11, 3, 1, 350000, 52325); -- VIP lounge
INSERT INTO inscxservicio VALUES (11, 4, 1, 180000, 18720); -- Souvenir

-- Inscripcion 12 (exp 12): servicios disponibles: 1,2,5
INSERT INTO inscxservicio VALUES (12, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (12, 2, 2, 200000, 38000); -- Alimentación x2

-- Inscripcion 13 (exp 7): servicios disponibles: 1,4,6
INSERT INTO inscxservicio VALUES (13, 6, 1,  80000,  4000); -- Seguro
INSERT INTO inscxservicio VALUES (13, 4, 1, 180000, 18720); -- Souvenir

-- Inscripcion 14 (exp 10): servicios disponibles: 1,2
INSERT INTO inscxservicio VALUES (14, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (14, 2, 1, 200000, 38000); -- Alimentación

-- Inscripcion 15 (exp 9): servicios disponibles: 1,2,3,5,7
INSERT INTO inscxservicio VALUES (15, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (15, 3, 1, 350000, 52325); -- VIP lounge
INSERT INTO inscxservicio VALUES (15, 5, 1, 120000,  6000); -- Traducción

-- Inscripcion 16 (exp 1): servicios disponibles: 1,2,3,4,5,7
INSERT INTO inscxservicio VALUES (16, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (16, 3, 1, 350000, 52325); -- VIP lounge

-- Inscripcion 17 (exp 7): servicios disponibles: 1,4,6
INSERT INTO inscxservicio VALUES (17, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (17, 4, 2, 180000, 18720); -- Souvenir x2
INSERT INTO inscxservicio VALUES (17, 6, 1,  80000,  4000); -- Seguro

-- Inscripcion 18 (exp 12): servicios disponibles: 1,2,5
INSERT INTO inscxservicio VALUES (18, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (18, 2, 1, 200000, 38000); -- Alimentación

-- Inscripcion 19 (exp 5): servicios disponibles: 1,4,6
INSERT INTO inscxservicio VALUES (19, 1, 1, 150000, 31500); -- Transporte

-- Inscripcion 20 (exp 10): servicios disponibles: 1,2
INSERT INTO inscxservicio VALUES (20, 2, 2, 200000, 38000); -- Alimentación x2
INSERT INTO inscxservicio VALUES (20, 1, 1, 150000, 31500); -- Transporte

-- Inscripcion 21 (exp 3): servicios disponibles: 1,3,6,7
INSERT INTO inscxservicio VALUES (21, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (21, 3, 1, 350000, 52325); -- VIP lounge

-- Inscripcion 22 (exp 11): servicios disponibles: 1,3,4,6,7
INSERT INTO inscxservicio VALUES (22, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (22, 4, 1, 180000, 18720); -- Souvenir

-- Inscripcion 23 (exp 9): servicios disponibles: 1,2,3,5,7
INSERT INTO inscxservicio VALUES (23, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (23, 5, 1, 120000,  6000); -- Traducción

-- Inscripcion 24 (exp 5): servicios disponibles: 1,4,6
INSERT INTO inscxservicio VALUES (24, 1, 1, 150000, 31500); -- Transporte
INSERT INTO inscxservicio VALUES (24, 6, 1,  80000,  4000); -- Seguro

-- ============================================================
--  PAGOS
--  Inscripciones 11,12,14,17,20,22,24 tienen saldo pendiente
--  → solo se registra pago parcial para que la consulta 6 retorne datos.
-- ============================================================

ALTER TRIGGER TRG_ACTUALIZAR_SALDO DISABLE;

INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 1,'REF-001-2026',3080000,DATE '2026-05-01','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 2,'REF-002-2026',1000000,DATE '2026-05-02','débito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 2,'REF-003-2026',1150000,DATE '2026-05-03','efectivo');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 3,'REF-004-2026',3870000,DATE '2026-05-03','billetera digital');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 4,'REF-005-2026',2780000,DATE '2026-05-04','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 5,'REF-006-2026',1980000,DATE '2026-05-05','efectivo');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 6,'REF-007-2026',2620000,DATE '2026-05-06','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 7,'REF-008-2026',1650000,DATE '2026-05-07','débito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 8,'REF-009-2026',2150000,DATE '2026-05-08','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES ( 9,'REF-010-2026',1380000,DATE '2026-05-07','efectivo');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (10,'REF-011-2026',2450000,DATE '2026-05-08','billetera digital');
-- Inscripcion 11: pago parcial → saldo queda en 1.000.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (11,'REF-012-2026',1460000,DATE '2026-05-09','crédito');
-- Inscripcion 12: pago parcial → saldo queda en 750.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (12,'REF-013-2026',1500000,DATE '2026-05-10','débito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (13,'REF-014-2026',1350000,DATE '2026-05-09','efectivo');
-- Inscripcion 14: pago parcial → saldo queda en 500.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (14,'REF-015-2026',1020000,DATE '2026-05-10','billetera digital');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (15,'REF-016-2026',3100000,DATE '2026-05-10','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (16,'REF-017-2026',2700000,DATE '2026-05-11','débito');
-- Inscripcion 17: pago parcial → saldo queda en 450.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (17,'REF-018-2026',1000000,DATE '2026-05-11','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (18,'REF-019-2026',2050000,DATE '2026-05-12','efectivo');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (19,'REF-020-2026',1800000,DATE '2026-05-12','billetera digital');
-- Inscripcion 20: pago parcial → saldo queda en 300.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (20,'REF-021-2026', 950000,DATE '2026-05-13','crédito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (21,'REF-022-2026',3350000,DATE '2026-05-13','crédito');
-- Inscripcion 22: pago parcial → saldo queda en 980.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (22,'REF-023-2026',1000000,DATE '2026-05-14','débito');
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (23,'REF-024-2026',2600000,DATE '2026-05-14','crédito');
-- Inscripcion 24: pago parcial → saldo queda en 350.000
INSERT INTO pago (id_inscripcion, referencia, valor_pagado, fecha_pago, tipo_pago) VALUES (24,'REF-025-2026',1000000,DATE '2026-05-15','efectivo');
ALTER TRIGGER TRG_ACTUALIZAR_SALDO ENABLE;
COMMIT;

-- ============================================================
--  5. CONSULTAS MÍNIMAS REQUERIDAS
-- ============================================================

-- CONSULTA 1A: Experiencias disponibles por CIUDAD
SELECT e.descripcion        AS experiencia,
       e.estado,
       e.precio_base,
       e.cupos_disponibles,
       est.nombre           AS estadio,
       est.ciudad,
       est.pais,
       p.equipo_local || ' vs ' || p.equipo_visitante AS partido,
       e.fecha_hora
  FROM experiencia e
  JOIN estadio est ON e.id_estadio = est.id_estadio
  JOIN partido  p  ON e.id_partido  = p.id_partido
 WHERE e.estado = 'disponible'
 ORDER BY est.ciudad, e.fecha_hora;

-- CONSULTA 1B: Experiencias disponibles por ESTADIO
SELECT e.descripcion        AS experiencia,
       e.estado,
       e.precio_base,
       e.cupos_disponibles,
       est.nombre           AS estadio,
       est.ciudad,
       p.equipo_local || ' vs ' || p.equipo_visitante AS partido,
       e.fecha_hora
  FROM experiencia e
  JOIN estadio est ON e.id_estadio = est.id_estadio
  JOIN partido  p  ON e.id_partido  = p.id_partido
 WHERE e.estado   = 'disponible'
   AND est.nombre = 'Estadio Azteca';

-- CONSULTA 1C: Experiencias disponibles por PARTIDO
SELECT e.descripcion        AS experiencia,
       e.estado,
       e.precio_base,
       e.cupos_disponibles,
       est.nombre           AS estadio,
       est.ciudad,
       p.equipo_local || ' vs ' || p.equipo_visitante AS partido,
       e.fecha_hora
  FROM experiencia e
  JOIN estadio est ON e.id_estadio = est.id_estadio
  JOIN partido  p  ON e.id_partido  = p.id_partido
 WHERE e.estado = 'disponible'
   AND (p.equipo_local = 'Colombia' OR p.equipo_visitante = 'Colombia');

-- CONSULTA 2: Clientes con mayor número de inscripciones
SELECT c.nombre_completo,
       c.correo,
       COUNT(i.id_inscripcion) AS total_inscripciones,
       SUM(i.costo_total)      AS gasto_total
  FROM cliente c
  JOIN inscripcion i ON c.id_cliente = i.id_cliente
 GROUP BY c.id_cliente, c.nombre_completo, c.correo
 ORDER BY total_inscripciones DESC, gasto_total DESC;

-- CONSULTA 3: Total de ingresos generados por cada guía
SELECT g.nombre                            AS guia,
       g.nivel,
       g.valor_hora,
       COUNT(DISTINCT ex.id_experiencia)   AS experiencias_lideradas,
       COUNT(i.id_inscripcion)             AS total_inscripciones,
       SUM(i.costo_total)                  AS ingresos_generados
  FROM guia g
  JOIN experiencia ex ON g.id_guia         = ex.id_guia_principal
  JOIN inscripcion i  ON ex.id_experiencia = i.id_experiencia
 GROUP BY g.id_guia, g.nombre, g.nivel, g.valor_hora
 ORDER BY ingresos_generados DESC;

-- CONSULTA 4: Experiencias más demandadas
SELECT ex.descripcion                                 AS experiencia,
       ex.estado,
       ex.precio_base,
       ex.cupos_disponibles,
       est.nombre                                     AS estadio,
       est.ciudad,
       est.pais,
       p.equipo_local || ' vs ' || p.equipo_visitante AS partido,
       COUNT(i.id_inscripcion)                        AS total_inscripciones,
       SUM(i.costo_total)                             AS ingresos_experiencia
  FROM experiencia ex
  JOIN estadio     est ON ex.id_estadio  = est.id_estadio
  JOIN partido     p   ON ex.id_partido  = p.id_partido
  LEFT JOIN inscripcion i ON ex.id_experiencia = i.id_experiencia
 GROUP BY ex.id_experiencia, ex.descripcion, ex.estado,
          ex.precio_base, ex.cupos_disponibles,
          est.nombre, est.ciudad, est.pais,
          p.equipo_local, p.equipo_visitante
 ORDER BY total_inscripciones DESC, ingresos_experiencia DESC;

-- CONSULTA 5: Total de impuestos cobrados por país sede
SELECT est.pais,
       COUNT(DISTINCT ex.id_experiencia)  AS total_experiencias,
       COUNT(i.id_inscripcion)            AS total_inscripciones,
       SUM(i.costo_total)                 AS ingresos_totales,
       ROUND(AVG(i.costo_total), 2)       AS ingreso_promedio
  FROM estadio est
  JOIN experiencia  ex ON est.id_estadio    = ex.id_estadio
  JOIN inscripcion  i  ON ex.id_experiencia = i.id_experiencia
 WHERE est.pais IN ('México','Estados Unidos','Canadá')
 GROUP BY est.pais
 ORDER BY ingresos_totales DESC;

-- CONSULTA 6: Clientes con pagos pendientes
SELECT c.nombre_completo,
       c.correo,
       c.telefono,
       i.num_reserva,
       ex.descripcion                AS experiencia,
       i.costo_total,
       i.saldo_pendiente,
       i.fecha_inscripcion
  FROM cliente c
  JOIN inscripcion  i  ON c.id_cliente     = i.id_cliente
  JOIN experiencia  ex ON i.id_experiencia = ex.id_experiencia
 WHERE i.saldo_pendiente > 0
 ORDER BY i.saldo_pendiente DESC;

-- CONSULTA 7: Cupos disponibles por experiencia
SELECT ex.descripcion                                 AS experiencia,
       ex.estado,
       ex.precio_base,
       ex.cupos_disponibles                           AS cupos_restantes,
       est.nombre                                     AS estadio,
       est.ciudad,
       p.equipo_local || ' vs ' || p.equipo_visitante AS partido,
       ex.fecha_hora
  FROM experiencia ex
  JOIN estadio est ON ex.id_estadio = est.id_estadio
  JOIN partido  p  ON ex.id_partido  = p.id_partido
 ORDER BY ex.cupos_disponibles DESC, ex.fecha_hora;