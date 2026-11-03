# Cautiva Spa · Back-end del BackOffice web (CS-BO-01)

Monolito modular con Spring Boot 4.1, Spring Modulith y Oracle. Las reglas del proyecto están en [CLAUDE.md](CLAUDE.md) y los scripts de la base en [database/README.md](database/README.md).

Requisitos: JDK 21 o superior en `JAVA_HOME` y Docker. Maven no hace falta: se usa `./mvnw`.

## 1. Levantar Oracle Free en Docker

Elija las dos claves y reemplácelas en los comandos; no las guarde en el repositorio.

```bash
docker run -d --name oracle-local -p 1522:1521 -e ORACLE_PASSWORD=<clave_system> -e APP_USER=SYNAPSIS_SPA_CAUTIVA -e APP_USER_PASSWORD=<clave_dueno> gvenzl/oracle-free:latest
```

Espere a que el log diga `DATABASE IS READY TO USE!`:

```bash
docker logs -f oracle-local
```

## 2. Instalar database/

Los scripts son de SQL*Plus en Windows-1252: no sirven ScriptUtils, JDBC ni el «Ejecutar script» de DBeaver. Copie la carpeta al contenedor:

```bash
docker cp database oracle-local:/database
```

Ejecute los pasos en este orden. Cada uno corre en `/tmp` porque `00` escribe un log en la carpeta actual:

```bash
docker exec -e NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252 oracle-local bash -c "cd /tmp && echo EXIT | sqlplus -s SYNAPSIS_SPA_CAUTIVA/<clave_dueno>@//localhost:1521/FREEPDB1 @/database/00_instalar_v5.sql"
```

```bash
docker exec -e NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252 oracle-local bash -c "cd /tmp && echo EXIT | sqlplus -s system/<clave_system>@//localhost:1521/FREEPDB1 @/database/14_cuenta_aplicacion.sql"
```

```bash
docker exec -e NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252 oracle-local bash -c "cd /tmp && echo EXIT | sqlplus -s SYNAPSIS_SPA_CAUTIVA/<clave_dueno>@//localhost:1521/FREEPDB1 @/database/13_permisos.sql"
```

```bash
docker exec -e NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252 oracle-local bash -c "cd /tmp && echo EXIT | sqlplus -s SYNAPSIS_SPA_CAUTIVA/<clave_dueno>@//localhost:1521/FREEPDB1 @/database/15_datos_iteracion1.sql"
```

Elija la clave de la cuenta del back-end (solo letras y números, empezando con una letra) y desbloquéela:

```bash
docker exec oracle-local bash -c "echo 'ALTER USER CAUTIVA_APP IDENTIFIED BY <clave_app> ACCOUNT UNLOCK;' | sqlplus -s system/<clave_system>@//localhost:1521/FREEPDB1"
```

El resultado esperado de cada paso está en [database/README.md](database/README.md): 55 tablas y ningún error ni objeto inválido.

## 3. Arrancar con el perfil dev

`dev` es el perfil por defecto y lee la conexión de `.env`, que git ignora. Cree el archivo a partir de la plantilla y escriba la clave de `CAUTIVA_APP` en `CAUTIVA_DB_PASSWORD`:

```bash
cp .env.example .env
```

```bash
./mvnw spring-boot:run
```

La aplicación queda en `http://localhost:8080`. El back-end se conecta como `CAUTIVA_APP`, nunca con la cuenta dueña.

Swagger UI está en `http://localhost:8080/swagger-ui/index.html` y abre sin sesión. Para probar los endpoints, primero haz `POST /api/v1/auth/login`; los POST, PUT y PATCH además piden la cabecera `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`.

## Perfiles

| Perfil | Archivo | Uso |
| --- | --- | --- |
| (común) | `application.yaml` | Conexión por variables de entorno, JPA, zona horaria y cookie de sesión |
| `dev` | `application-dev.yaml` | PC de desarrollo: `.env`, Swagger UI, logs SQL y adaptadores falsos de APISPERU e Izipay |
| `sandbox` | `application-sandbox.yaml` | APIs reales de APISPERU e Izipay en modo de pruebas, con secretos por variables de entorno |
| `prod` | `application-prod.yaml` | Producción: sin Swagger UI, cookie Secure y adaptadores reales; todo por variables de entorno del servidor |

Los adaptadores falsos solo existen en `dev` y en las pruebas. En `sandbox` y `prod`, si falta una variable obligatoria la aplicación no arranca.

## Arrancar con el perfil sandbox

`sandbox` no lee `.env`: exporte las variables en la terminal. Use credenciales de prueba de cada proveedor, nunca las de producción.

| Variable | Obligatoria | Para qué |
| --- | --- | --- |
| `CAUTIVA_DB_URL`, `CAUTIVA_DB_USER`, `CAUTIVA_DB_PASSWORD`, `CAUTIVA_DB_SCHEMA` | Sí | Conexión a Oracle, como en `dev` |
| `APISPERU_TOKEN` | Sí | Token de la consulta de DNI y RUC |
| `APISPERU_FACTURACION_TOKEN` | Sí | Token de la empresa en la facturación de APISPERU |
| `IZIPAY_USUARIO`, `IZIPAY_CLAVE` | Sí | Usuario y contraseña de TEST de la API REST en el Back Office de Izipay |
| `IZIPAY_HMAC_CLAVE` | Sí | Clave con la que Izipay firma `kr-hash`; compruebe en el Back Office cuál corresponde a la notificación |
| `CAUTIVA_ENLACE_CLAVE` | Para los enlaces públicos | Firma los enlaces públicos de los comprobantes; sin ella no se generan |
| `CAUTIVA_URL_PUBLICA` | No | URL base de esos enlaces; por defecto `http://localhost:8080` |
| `CAUTIVA_EMPRESA_RUC`, `CAUTIVA_EMPRESA_RAZON_SOCIAL`, `CAUTIVA_EMPRESA_DIRECCION`, `CAUTIVA_EMPRESA_UBIGEO`, `CAUTIVA_EMPRESA_DEPARTAMENTO`, `CAUTIVA_EMPRESA_PROVINCIA`, `CAUTIVA_EMPRESA_DISTRITO` | Para emitir | Datos del emisor en el comprobante y su PDF |
| `CAUTIVA_ARCHIVOS_DIR` | No | Carpeta de los XML y CDR; por defecto `./archivos` |
| `CAUTIVA_COOKIE_SECURE` | No | `false` para probar en `http://localhost`; por defecto `true` |
| `APISPERU_CONSULTA_URL`, `APISPERU_FACTURACION_URL`, `IZIPAY_API_URL` | No | Cambian las URL de los proveedores |

```bash
SPRING_PROFILES_ACTIVE=sandbox ./mvnw spring-boot:run
```

Izipay debe poder llamar a `POST /api/v1/pagos/notificaciones`: configure esa URL pública en su Back Office, en las reglas de notificación. Mientras no llegue la notificación, los cobros digitales siguen PENDIENTES.

Los contratos de los proveedores aún no se probaron contra sus servicios: lo que falta verificar está en DF-49 de [docs/NOTAS_IT1.md](docs/NOTAS_IT1.md).

## Pruebas

```bash
./mvnw verify
```

Solo necesita Docker en marcha: no usa la base local ni `.env`. `OracleTestcontainer` levanta su propio `gvenzl/oracle-free` e instala `database/`; la primera vez tarda varios minutos.
