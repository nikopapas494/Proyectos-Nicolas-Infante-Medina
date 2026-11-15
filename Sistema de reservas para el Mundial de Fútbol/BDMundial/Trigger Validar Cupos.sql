--------------------------------------------------------
-- Archivo creado  - martes-junio-02-2026   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Trigger TRG_VALIDAR_CUPOS
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE TRIGGER "IS331509"."TRG_VALIDAR_CUPOS" 
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
ALTER TRIGGER "IS331509"."TRG_VALIDAR_CUPOS" ENABLE;
