--------------------------------------------------------
-- Archivo creado  - martes-junio-02-2026   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Trigger TRG_INS_AUDITORIA
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE TRIGGER "IS331509"."TRG_INS_AUDITORIA" 
AFTER INSERT OR UPDATE OR DELETE ON BOOKS 
FOR EACH ROW 
BEGIN
  IF INSERTING THEN
    INSERT INTO TRACK_AUDITOR (
      USUARIO,
      VALOR_ANTERIOR,
      VALOR_NUEVO,
      OPERACION,
      FECHA,
      IP
    ) VALUES (
      sys_context('USERENV','SESSION_USER'),
      NULL,
      :NEW.AUTHOR_LAST_NAME,
      'INSERT',
      SYSDATE,
      sys_context('USERENV','IP_ADDRESS')
    );

  ELSIF UPDATING THEN
    INSERT INTO TRACK_AUDITOR (
      USUARIO,
      VALOR_ANTERIOR,
      VALOR_NUEVO,
      OPERACION,
      FECHA,
      IP
    ) VALUES (
      sys_context('USERENV','SESSION_USER'),
      :OLD.AUTHOR_LAST_NAME,
      :NEW.AUTHOR_LAST_NAME,
      'UPDATE',
      SYSDATE,
      sys_context('USERENV','IP_ADDRESS')
    );

  ELSIF DELETING THEN
    INSERT INTO TRACK_AUDITOR (
      USUARIO,
      VALOR_ANTERIOR,
      VALOR_NUEVO,
      OPERACION,
      FECHA,
      IP
    ) VALUES (
      sys_context('USERENV','SESSION_USER'),
      :OLD.AUTHOR_LAST_NAME,
      NULL,
      'DELETE',
      SYSDATE,
      sys_context('USERENV','IP_ADDRESS')
    );
  END IF;
END;
/
ALTER TRIGGER "IS331509"."TRG_INS_AUDITORIA" ENABLE;
