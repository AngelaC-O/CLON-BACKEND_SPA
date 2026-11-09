# Notas de la Iteración 2 · Back-end

Desde el 08/11/2026 las dudas y decisiones van aquí. `docs/NOTAS_IT1.md` queda como historia y no se edita; los números D-xx y DF-xx de la Iteración 1 se conservan y los nuevos siguen la numeración (D-14, DF-58...).

## Dudas

Abiertas de la Iteración 1:

- **D-05. `codigo_operacion` del enlace de pago.** `ServicioPago.confirmar` busca con `findByCodigoOperacion`, pero en CUS07 ningún mensaje guarda ese código antes del pago. En B4 se agregó la sobrecarga `registrarEnlace(enlacePago, fechaExpiracion, codigoOperacion)`, porque sin el código el webhook no encuentra la transacción. Sigue sin confirmarse.
- **D-09. Almuerzo de la especialista (Iteración 3).** `HORARIO_PERSONAL.minutos_almuerzo` da los minutos, pero no a qué hora, así que las horas libres y la agenda no lo descuentan. Queda para la Iteración 3.
- **DF-52. Importe del comprobante con adelanto.** El comprobante y su PDF llevan `subtotal + igv`, no `Venta.total`, que ya resta adelanto y saldo a favor. SUNAT trata los anticipos con campos propios, que no se emiten. Hay que revisarlo con contabilidad.
- **DF-49. Reporte de liquidación y claves TEST de Izipay.** Faltan las columnas del reporte de liquidación que se importa (CUS47) y las claves TEST de Izipay, que la dueña aún no solicita. Mientras tanto se usa el adaptador falso.
- **DATOS-01. Datos del emisor.** Ya se recibieron. Se cargan solo en producción, por las variables `CAUTIVA_EMPRESA_*` y `SEDE.direccion`; en desarrollo no se tocan ni se escriben en el repositorio.

Nuevas (08/11/2026, al preparar el v5.1):

- **D-14. RN03: la especialista queda libre al terminar.** Ver DF-58: Oracle sigue bloqueando a la especialista durante la limpieza del recurso. ¿Se cambia `PKG_AGENDA` (sería otra migración) o se acepta así en esta iteración?
- **D-15. Auditoría de la reapertura.** El diccionario dice que la historia de las reaperturas «queda en AUDITORIA», pero `TRG_AUD_CAJA` (script 10, generado) no conoce las tres columnas nuevas: registra `estado: CERRADA -> ABIERTA` y el cierre que se limpia, no el motivo ni quién reabrió (eso queda solo en la fila de CAJA, y solo la última vez). La hoja «Cambios v5.1» no lo pide y no se tocó. ¿Se regenera `TRG_AUD_CAJA`?
- **D-16. «Caja del día» en la reapertura.** RN30 habla de reabrir la caja del día, pero ni la hoja «Cambios v5.1» ni `TRG_CAJA_REGLAS` lo exigen: Oracle deja reabrir cualquier caja cerrada si no hay otra ABIERTA en la sede. Por ahora lo validará Java (CUS46). ¿Se agrega también en Oracle?
- **D-17. `99_eliminar_v5.sql`.** Solo borra las 55 tablas de v5; SOLICITUD_ANULACION y CONSUMO_PENDIENTE quedan. `database/README.md` indica el `DROP TABLE` a mano. ¿Se actualiza 99 la próxima vez que se toque `database/`?
- **D-18. Base local.** La base de Docker (`localhost:1522`, dueño `SYNAPSIS_SPA_CAUTIVA`) sigue en v5: en esta tarea 17 solo se corrió en un contenedor limpio de pruebas. Para usarla con el back-end de la Iteración 2 hay que correr ahí `17_datos_iteracion2.sql` como dueño.

## Diferencias encontradas

- **DF-58. RN03 ajustada frente a `PKG_AGENDA`.** RN03 (08/11) dice que la especialista queda libre al terminar el servicio y que solo el recurso queda bloqueado por la limpieza. Pero `TRG_CITA_REGLAS` guarda en `CITA.fecha_hora_fin` el fin con la limpieza, y `PKG_AGENDA.validar_cruce` usa ese fin para la especialista y para el recurso; `GestorDisponibilidadAgenda` hace lo mismo en Java. La hoja «Cambios v5.1» no cambia ni `PKG_AGENDA` ni `TRG_CITA_*`, así que no se tocaron (D-14). Las pruebas Java ajustadas siguen ese comportamiento (por ejemplo, la agenda de la especialista ocupada de 10:00 a 10:40).
- **DF-59. Casos de 20 que cambian con el v5.1.** Con 17, `20_pruebas_reglas_v5.sql` da 142 OK y 2 FALLO, sin editarlo: «El fin incluye la limpieza: 10:00 + 30 + 30 = 11:00 (RN03)» (sale 10:40) y «Mismo sillón a las 10:45 con otra especialista (aún en limpieza)» (pasa). Sus recursos de prueba toman el DEFAULT de `minutos_limpieza`, que ahora es 10 (cambio 1), así que el sillón queda libre a las 10:40. Los demás casos, incluido «Reabrir la caja cerrada» (sin motivo, `-20060`) y «Vender en la caja cerrada» (`-20042`), siguen pasando.
- **DF-60. `CONSUMO_PENDIENTE.cantidad_pendiente`.** El diccionario la marca como derivada, pero la hoja «Cambios v5.1» no le pide al trigger que la calcule: `CK_CONSUMO_PEND_CANTIDADES` solo la valida. Java la envía (estimada − descontada).
- **DF-61. Aprobar una solicitud con su nota de crédito.** Como una solicitud resuelta no cambia (`-20102`), la nota de crédito se emite primero y su `id_nota_credito` va en el mismo UPDATE que pasa la solicitud a APROBADA. Si SUNAT había rechazado el comprobante, se aprueba con `id_nota_credito` nulo (lo permite `CK_SOL_ANUL_NOTA_CREDITO`).
- **DF-62. Orden de las validaciones al reabrir la caja.** `TRG_CAJA_REGLAS` revisa primero el motivo (`-20060`) y después que sea la Administradora (`-20003`). Así una reapertura sin motivo sigue siendo el `-20060` de siempre (el caso «Reabrir la caja cerrada» de 20 no cambia) y la de una recepcionista con motivo es 403.
- **DF-63. Otra caja ABIERTA al reabrir.** Que no haya otra caja ABIERTA en la sede lo garantiza el índice `UQ_CAJA_ABIERTA_SEDE` (`ORA-00001`), no el trigger: una regla por fila sobre CAJA no puede consultar CAJA (tabla mutante). Java lo valida antes y traduce el `ORA-00001` a 409.

## Decisiones tomadas

Dudas de la Iteración 1 que se cierran (08/11/2026):

- **D-04** (tabla `PAQUETE_SERVICIO`) se cierra con CUS59 Registrar Paquete: PAQUETE_SERVICIO entra en las tablas de la Iteración 2.
- **D-06** (elegir un PERSONAL sin usuario en CUS52) se cierra con `GET /usuarios/personal-sin-usuario` (CUS52 modificado It. 2).
- **D-07** (quitarle permisos a ADMINISTRADORA) se cierra con RN48.
- **D-08** (paso de las horas libres) se cierra con RN51 y el PARAMETRO `AGENDA_PASO_MIN` (15).
- **D-10** (citas en el pasado y atenciones de otra especialista) se cierra con RN49 y RN50.
- **D-12** (canal del enlace de CUS20) se cierra con `mailto` (CUS20 modificado It. 2, 08/11).
- **D-13 y DF-51** (cobro digital rechazado o pendiente) se cierran con RN52.
- **Duda 2 del front-end** se cierra con los GET de los CUS modificados.

Decisiones del equipo del 08/11/2026:

- Anulación por solicitud y bandeja: la recepcionista pide anular (CUS22) y la Administradora aprueba o rechaza desde la bandeja (CUS23) con SOLICITUD_ANULACION (RN32).
- Devolución manual de Yape y Plin desde la aplicación de la Administradora, registrando el número de operación; la tarjeta por la pasarela; el efectivo desde caja; o saldo a favor (RN44).
- Reapertura de caja en lugar de `VENTA.id_usuario_autoriza` (RN30, CUS46): la columna queda obsoleta (CK_VENTA_AUTORIZA_OBSOLETA).
- Consumo automático de insumos al finalizar la atención, con CONSUMO_PENDIENTE para lo que no alcanzó (RN53, RN54).
- Limpieza de hasta 10 minutos (RN03, LIMPIEZA_MAX_MIN = 10).
- Reporte de liquidación importado para conciliar (CUS47).
- Panel de avisos (`AvisoProvider`, `GET /avisos`, CUS50).
- Reportes adelantados a la Iteración 2 (módulo `reportes`, CUS48 y CUS49).
- **JDK.** Se trabaja con el JDK 26 y el objetivo sigue siendo Java 21. Comprobado el 08/11/2026: `./mvnw -v` muestra «Java version: 26.0.2.1» (`JAVA_HOME` = `C:\Users\Laptop\.jdks\openjdk-26.0.2.1`; en esta PC no está definido de forma permanente y hubo que fijarlo en la consola), `pom.xml` sigue con `<java.version>21</java.version>` y el POM efectivo tiene `maven.compiler.release` = 21, así que Maven compila con `--release 21`. Ninguna prueba falló por el JDK 26 (ver «Verificación del cierre»).

Migración v5 → v5.1 (`database/17_datos_iteracion2.sql`, 08/11/2026):

- **Paso previo.** Antes de cambiar nada, 17 cuenta las ventas con `id_usuario_autoriza`; si hay alguna (y aún no existe CK_VENTA_AUTORIZA_OBSOLETA), se detiene con `-20100` sin tocarlas. En el contenedor de pruebas no había ninguna. Si pasa en otra base, se anota aquí y se decide antes de volver a correrlo.
- **Códigos de error nuevos** (el mayor usado en 06, 08 y 09 era `-20099`): `-20100` VENTA.id_usuario_autoriza obsoleta; `-20101` la solicitud es de una venta CERRADA (o la venta no existe); `-20102` la solicitud nace PENDIENTE, no cambia de venta y, resuelta, no cambia; `-20103` el consumo pendiente nace PENDIENTE y, resuelto, no cambia; `-20104` solo almacén o la Administradora resuelven un consumo pendiente; `-20105` regularizar exige una SALIDA del mismo insumo, sede y atención; `-20106` solo lo usa 17 al comprobar que VENTAS:EDITAR siga solo en ADMINISTRADORA. Se reutilizan `-20003`, `-20031`, `-20042` y `-20060`. También están en `database/README.md`.
- **Triggers.** `TRG_CAJA_REGLAS`, `TRG_KARDEX_APLICAR` y `TRG_VENTA_REGLAS` parten del código de 09 y solo cambian en lo que dice la hoja (cambios 11 a 13). Los triggers nuevos fijan desde PKG_SESION quién y cuándo (pide, resuelve, registra) y limpian al insertar los datos de la resolución. `TRG_AUD_SOLICITUD_ANULACION` y `TRG_AUD_CONSUMO_PENDIENTE` siguen el molde generado de 10.
- **Consumo automático (RN17, RN53).** `TRG_KARDEX_APLICAR` admite la SALIDA con `id_atencion` cuando el usuario de la sesión es la especialista de esa atención (`USUARIO.id_personal = ATENCION.id_personal`). Otra especialista, la recepcionista o la misma salida sin `id_atencion` siguen siendo `-20031`.
- **RN54.** `12_datos_iniciales.sql` no carga insumos (los 6 productos son de VENTA en UND) ni filas de SERVICIO_INSUMO, así que no hubo nada que corregir. 17 solo avisa si encuentra un insumo con una unidad que no sea ML, G o UND o un consumo estimado con decimales; no convierte datos.
- **Permisos.** 12 no daba a RECEPCIONISTA ni `CATALOGO:VER` ni `VENTAS:ANULAR`: 17 los agrega. `VENTAS:EDITAR` sigue solo en ADMINISTRADORA (17 lo comprueba y se detiene si no). ADMINISTRADORA no se toca.
- **Usuaria `almacen`.** PERSONA (DNI de prueba 70000006) + PERSONAL ENCARGADA_ALMACEN en Pichanaki Centro, con el mismo horario de 15, y USUARIO `almacen` con el hash de la clave temporal de 15 y `debe_cambiar_clave = 'S'`.
- **Objetos dependientes.** Después de los ALTER TABLE, 17 recompila lo inválido (`DBMS_UTILITY.COMPILE_SCHEMA` solo de lo inválido) para terminar con 0 inválidos.
- **Pruebas de la migración.** `database/21_pruebas_v51.sql` (58 casos, termina en ROLLBACK) y `MigracionV51Test`, que lo corre en Testcontainers después de 17 dentro de `./mvnw verify`. `OracleTestcontainer` instala 17 después de 15. `InstalacionBaseDatosTest` espera ahora 57 tablas y 131 FK.
- **16 no cambia.** `16_verificar_cuenta_aplicacion.sql` no cuenta tablas.
- **Pruebas Java ajustadas al v5.1** (solo datos de prueba, sin lógica nueva): `AgendaFlujoTest` (fin con 10 minutos de limpieza: 10:40 y 10:55; cruces a las 10:20 y 10:35; sin cruce a las 10:40), `MapeoEntidadesTest` (recurso de 10 minutos: 10:40 y 10:50) y `SeguridadCatalogoFlujoTest` (recurso de 10 minutos y rechazo con 11; la recepcionista ahora ve `GET /areas` por `CATALOGO:VER` y sigue sin poder `POST /areas`).

Resultado en un contenedor limpio (`gvenzl/oracle-free:23.26.2`, `TZ=America/Lima`, 08/11/2026), en el orden 00 → 14 → 13 → 15 → 17 → 16:

| Script | Resultado |
| --- | --- |
| `00_instalar_v5.sql` | 55 tablas, 125 triggers, 0 errores (v5) |
| `14`, `13`, `15` | Sin errores |
| `17_datos_iteracion2.sql` | 57 tablas; P 57, R 131, U 35, C 183; 105 IX_; TRIGGER 129; 0 inválidos; 0 errores. Corrido dos veces: la segunda no cambió nada y terminó igual. |
| `97_prueba_rapida_v5.sql` | 14 OK, 0 FALLO |
| `20_pruebas_reglas_v5.sql` | 142 OK, 2 FALLO (DF-59: limpieza de 10 minutos; no se editó) |
| `21_pruebas_v51.sql` | 58 OK, 0 FALLO |
| `16_verificar_cuenta_aplicacion.sql` (como CAUTIVA_APP) | 11 OK, 0 FALLO |

Lo esperado eran 14, 144 y 11 OK; la diferencia en 20 son los dos casos de DF-59.

### Verificación del cierre

`./mvnw -q verify` con el JDK 26 (08/11/2026): la primera corrida, ya con 17 instalado por `OracleTestcontainer`, dio 7 pruebas en rojo, todas por los datos del v5.1 y no por el JDK: limpieza de 15 minutos rechazada por `CK_RECURSO_MINUTOS_LIMPIEZA` (`ORA-02290`) o por `LIMPIEZA_MAX_MIN` (422), fines calculados con 30 minutos de limpieza y `GET /areas` que ahora devuelve 200 a la recepcionista. Con los datos de prueba ajustados (ver arriba), la segunda corrida terminó bien: 122 pruebas, 0 fallos, 0 errores, 0 omitidas (incluye `MigracionV51Test` y `ModularidadTest`). Ninguna falla por el JDK 26.

Este repositorio es solo el back-end: `npm run build` y `npm test` del front-end no se corrieron aquí.
