--------------------------------------------------------
-- Archivo creado  - martes-junio-02-2026   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Trigger TRG_ACTUALIZAR_SALDO
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE TRIGGER "IS331509"."TRG_ACTUALIZAR_SALDO" 
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
ALTER TRIGGER "IS331509"."TRG_ACTUALIZAR_SALDO" ENABLE;
