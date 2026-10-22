# Pedidos360 — EP3 RabbitMQ

Backend local de demostracion para gestionar productos y pedidos y publicar tareas asincronas mediante RabbitMQ. Contiene cuatro microservicios Spring Boot 4.0.8 con Java 21, dos bases PostgreSQL y un broker RabbitMQ 4.2.9 con panel de administracion.

**Ampliacion del bloque 2:** ver [AVANCE_BLOQUE_2.md](docs/AVANCE_BLOQUE_2.md) para el nuevo cluster de dos nodos, Docker de aplicaciones, politicas y procesamiento de archivos. El procedimiento de este README conserva el modo local original.

## Alcance implementado

- Catalogo: crear, listar, consultar y actualizar productos; descuento de stock validado y con bloqueo de escritura dentro de una transaccion.
- Pedidos: crear y consultar pedidos. Consulta al catalogo y calcula el total usando el precio real; conserva una copia del nombre y precio del producto.
- Mensajeria: publicacion de comandos EMAIL, KITCHEN e INVOICE sobre pedidos existentes. Usa mensajes persistentes, IDs de trazabilidad, publisher confirms y devolucion de mensajes no enrutados.
- Consumidores separados por dominio con `@RabbitListener`, persistencia de resultados de texto por dominio, ACK manual, manejo de errores y un reintento mediante reencolado antes de DLQ.
- Inspeccion REST de una carta muerta, con log y reencolado en su DLQ para conservarla.
- Administrador REST: declarar y eliminar colas, exchanges y bindings, validar entradas y traducir errores del broker a HTTP.

## Servicios y puertos

| Componente | Puerto local | Health |
|---|---:|---|
| catalog-service | 8081 | http://localhost:8081/actuator/health |
| orders-service | 8082 | http://localhost:8082/actuator/health |
| notify-service | 8083 | http://localhost:8083/actuator/health |
| rabbit-admin-service | 8084 | http://localhost:8084/actuator/health |
| PostgreSQL catalogo | 5433 | Docker healthcheck |
| PostgreSQL pedidos | 5434 | Docker healthcheck |
| RabbitMQ AMQP | **45672** | Docker healthcheck |
| RabbitMQ Management | 15672 | http://localhost:15672 |

El puerto 45672 de Windows se redirige al 5672 del contenedor. En el modo original de este README los servicios Spring se ejecutan en Windows. La ampliacion compose.stack.yml agrega Dockerfiles y contenedores de aplicaciones en un entorno aislado, con puertos 8091-8094.

## Requisitos

- JDK 21, con `bin/java.exe` y `bin/javac.exe`.
- Docker Desktop iniciado, usando contenedores Linux, y Docker Compose v2.
- Windows PowerShell 5.1 o PowerShell 7 para los scripts incluidos.
- Acceso a Maven Central y al registro de imagenes la primera vez.
- Postman para importar la coleccion incluida.

Maven global no es necesario: cada servicio conserva su Maven Wrapper. Los scripts guardan Maven y sus dependencias en `.cache` dentro del proyecto, para evitar llenar C si el proyecto esta en otro disco.

## Configuracion inicial

Desde la raiz del proyecto, por ejemplo `F:\Pedidos360`:

```powershell
Copy-Item .env.example .env
notepad .env
```

Reemplazar los tres valores `CAMBIAR_...` por contrasenas locales elegidas por quien ejecuta el proyecto. No publicar `.env`. El usuario RabbitMQ es `pedidos360`.

**Si ya existe un `.env` funcionando, conservarlo y no ejecutar Copy-Item sobre el.** Conservar las claves originales de las bases y RabbitMQ: cambiar `.env` no modifica automaticamente las credenciales almacenadas en volumenes ya inicializados.

Formato admitido por los scripts: `NOMBRE=valor`, una variable por linea; comentarios en lineas que empiezan por `#`. Usar valores simples sin interpolacion `$` para que Docker Compose y los scripts interpreten la misma clave. Las claves se cargan en el proceso del servicio; Spring no lee `.env` automaticamente.

## Arranque

Desde la raiz:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize.ps1
```

Esperar a que los tres contenedores esten `healthy`. Luego abrir **cuatro terminales**, situadas en la raiz del proyecto, y ejecutar un servicio en cada una. Sustituir `-JavaHome` si el JDK esta en otra ruta:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Service.ps1 -Service catalog-service -JavaHome 'F:\jdk-21\jdk-21.0.12.1+1'
```

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Service.ps1 -Service notify-service -JavaHome 'F:\jdk-21\jdk-21.0.12.1+1'
```

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Service.ps1 -Service orders-service -JavaHome 'F:\jdk-21\jdk-21.0.12.1+1'
```

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Service.ps1 -Service rabbit-admin-service -JavaHome 'F:\jdk-21\jdk-21.0.12.1+1'
```

Esperar al mensaje `Started ...Application` de cada servicio. Consultar `/actuator/health`, no solo `/actuator/health/readiness`, para comprobar tambien los contribuyentes de infraestructura. Iniciar notify antes de publicar: es el servicio que declara la topologia.

Para detener las aplicaciones usar Ctrl+C en cada terminal. Para detener infraestructura sin eliminar datos:

```powershell
docker compose stop
```

Si un puerto HTTP esta ocupado, detener la instancia previa antes de iniciar otra.

## Verificacion Maven

Con Docker y los servicios de infraestructura activos:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Verify.ps1 -JavaHome 'F:\jdk-21\jdk-21.0.12.1+1'
```

Ejecuta `clean verify` en los cuatro proyectos y se detiene ante el primer error. Las pruebas Java incluyen `contextLoads` de Spring Boot, que necesitan configuracion e infraestructura local, y cuatro pruebas unitarias de TareaService sobre archivos, deduplicacion y validacion. No equivalen a una suite automatizada de todas las reglas de negocio.

Tambien se puede ejecutar cada Wrapper manualmente, definiendo antes JAVA_HOME y las variables de contrasena correspondientes; ver los `application.properties` de cada servicio.

## Pruebas Postman

Importar `postman/Pedidos360_EP3.postman_collection.json`. Es una coleccion nueva preparada desde las rutas del codigo, no una exportacion de la coleccion personal usada durante el desarrollo.

Ejecutar la coleccion completa con Collection Runner, una iteracion y demora de **500 ms** entre solicitudes. Cada peticion tiene una comprobacion del estado HTTP; las principales comprueban campos de respuesta. La creacion de producto y pedido captura sus IDs automaticamente. Se generan nombres de recursos administrativos de prueba por ejecucion.

La carpeta de fallos publica comandos con `falloControlado=true`; las peticiones siguientes inspeccionan las DLQ. Las colas de errores conservan mensajes, por lo que una inspeccion puede mostrar un mensaje de una ejecucion anterior. Para correlacionar el caso actual, comparar `eventId` con `messageId` en logs o RabbitMQ Management.

Los fallos por indisponibilidad de infraestructura requieren pruebas manuales separadas: detener catalogo y crear pedido (503), o detener RabbitMQ e intentar publicar (503). Reiniciar el componente despues. No detener infraestructura durante la ejecucion normal de la coleccion.

## Topologia RabbitMQ

| Dominio | Cola principal | Routing key direct | Patron topic | DLQ |
|---|---|---|---|---|
| EMAIL | q.cmd.email | email.send | email.# | q.cmd.email.dlq |
| KITCHEN | q.cmd.kitchen | kitchen.ticket | kitchen.# | q.cmd.kitchen.dlq |
| INVOICE | q.cmd.invoice | invoice.gen | invoice.# | q.cmd.invoice.dlq |

Exchanges durables: `cmd.direct` (direct), `cmd.topic` (topic), `cmd.dead.dlx` (direct). Cada principal fija su DLX y dead-letter routing key. `RabbitConfig` declara exchanges como beans y agrupa queues/bindings en beans `Declarables` por caso de uso; los nombres comunes estan en `RabbitNames`.

Flujo correcto: POST comando → productor → exchange → cola → consumidor → ACK. Fallo controlado: NACK con requeue → entrega marcada redelivered → nuevo fallo → NACK sin requeue → DLX → DLQ. La marca de redelivery limita el reintento de esta demostracion; no es un contador persistente de intentos de negocio y una entrega recuperada tras desconexion puede llegar ya marcada.

Un 202 significa confirmacion del broker y ausencia de devolucion por no enrutado; **no** confirma que la tarea haya terminado. Un timeout de confirmacion puede dejar resultado incierto; reenviar una solicitud crea otro eventId y puede duplicar tareas.

## Endpoints

| Servicio | Metodo | Ruta | Resultado principal |
|---|---|---|---|
| Catalogo | POST | /api/productos | 201 |
| Catalogo | GET | /api/productos | 200 |
| Catalogo | GET | /api/productos/{id} | 200 / 404 |
| Catalogo | PUT | /api/productos/{id} | 200 / 400 / 404 |
| Catalogo | POST | /api/productos/{id}/stock/descontar | 200 / 400 / 404 / 409 |
| Pedidos | POST | /api/pedidos | 201 / 400 / 404 / 409 / 503 |
| Pedidos | GET | /api/pedidos | 200 |
| Pedidos | GET | /api/pedidos/{id} | 200 / 404 |
| Pedidos | POST | /api/pedidos/{id}/comandos/{destino} | 202 / 400 / 404 / 502 / 503 |
| Notificaciones | POST | /api/dlq/{dominio}/inspeccionar | 200 / 400 / 503 |
| Administrador | POST | /api/admin/queues | 201 / 400 / 409 |
| Administrador | DELETE | /api/admin/queues/{nombre} | 204 / 400 / 409 |
| Administrador | POST | /api/admin/exchanges | 201 / 400 / 409 |
| Administrador | DELETE | /api/admin/exchanges/{nombre} | 204 / 400 / 409 |
| Administrador | POST | /api/admin/bindings | 201 / 400 / 404 |
| Administrador | DELETE | /api/admin/bindings | 204 / 400 / 404 |

Destinos/dominios: EMAIL, KITCHEN, INVOICE en mayusculas. Exchanges del comando: DIRECT o TOPIC. Todos los servicios pueden responder 503 si una dependencia necesaria no esta disponible. El DELETE de bindings recibe el mismo JSON que su POST. La declaracion repetida con los mismos atributos es idempotente; cambiar atributos de una cola existente puede devolver 409. No eliminar las colas funcionales para probar el administrador: usar recursos `demo.*`.

## Evidencia de los indicadores EP3

| Indicador | Evidencia en el codigo |
|---|---|
| IE3.1 Configuracion centralizada | notify/config/RabbitNames.java, RabbitConfig.java; propiedades messaging.exchange de orders |
| IE3.2 Queue, Exchange, Binding | notify/config/RabbitConfig.java: beans de exchanges y Declarables por dominio |
| IE3.3 Separacion | paquetes config, consumer, messaging, pedido y admin |
| IE3.4 Consumidores por dominio | EmailConsumer, KitchenConsumer, InvoiceConsumer con @RabbitListener |
| IE3.5 ACK y errores | ProcesadorMensaje: basicAck/basicNack; DlqService: inspeccion y log |
| IE3.6 API administradora | RabbitAdminController |
| IE3.7 Servicio administrador | RabbitAdminService; RabbitAdminConfig configura la dependencia |
| IE3.8 Validaciones | AdminRequests, @Valid y validacion de nombres al eliminar |

## Limites de esta version

- Es un avance del bloque 2, no el proyecto semestral completo. Incluye ahora configuracion de cluster de dos nodos y Docker de aplicaciones, pendiente de ejecutar y evidenciar. No incluye Angular, Entra ID, JWT, API Gateway, RDS, EC2 configurado ni Kafka.
- Pedidos permanecen en CREADO. Consultar stock al crear no reserva unidades. El endpoint de descuento funciona por separado; publicar un comando no descuenta stock ni cambia el estado del pedido.
- EMAIL, KITCHEN e INVOICE generan archivos de texto persistentes: notificacion preparada, ticket de cocina y comprobante interno. No envian correos, imprimen tickets ni generan comprobantes tributarios o PDF.
- No hay outbox ni transaccion distribuida. Existe deduplicacion por archivo/eventId dentro de cada dominio y volumen compartido por una instancia; no se promete procesamiento exactamente una vez entre replicas.
- Las APIs locales no tienen autenticacion. El administrador escucha solo en loopback; el despliegue cloud requerira seguridad y configuracion adicionales.
- El administrador admite TTL/maxLength para las colas que crea. Las politicas funcionales se importan al stack de dos nodos; el compose.yml original no las aplica automaticamente.

## Entrega GitHub

Desde la raiz, revisar `.gitignore`, exportar cualquier evidencia adicional y ejecutar:

```powershell
git init
git add .
git status
git diff --cached --stat
git commit -m 'Prepara entrega EP3 RabbitMQ'
git branch -M main
```

Crear un repositorio vacio en GitHub, conectar su URL con `git remote add origin URL_DEL_REPOSITORIO` (o `git remote set-url origin ...` si ya existe) y ejecutar `git push -u origin main`. Comprobar que el docente pueda acceder al enlace. No subir `.env`, `.cache`, `target`, claves ni tokens. Los HELP.md de Initializr se conservan como referencia generada; el procedimiento de este README es el aplicable al proyecto.
