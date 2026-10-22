# Avance del bloque evaluativo 2

Esta ampliacion agrega configuracion reproducible para un cluster RabbitMQ de dos nodos, politicas de retencion, empaquetado Docker de las cuatro aplicaciones y resultados persistentes de los consumidores. El compose original se conserva para poder volver al entorno ya probado.

## Que ejecutar primero

1. Extraer esta version en otra carpeta. Copiar el .env que ya funciona a esa carpeta, sin sobrescribir las claves existentes.
2. El script genera y conserva automaticamente una cookie aleatoria en .env si falta o tiene el valor de ejemplo. Ambos nodos usan la misma cookie. No se muestra en consola ni se publica. No cambiarla una vez creados los volumenes del cluster.

3. Arrancar solo el cluster, desde la raiz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Stack.ps1
```

El script espera ambos nodos, comprueba que la API ve a ambos miembros e importa seis colas, tres exchanges, nueve bindings y dos politicas. Si falla, detenerse y conservar los logs.

4. Abrir http://localhost:15674 y comprobar los dos nodos `rabbit@rabbit1` y `rabbit@rabbit2`. El segundo panel esta en http://localhost:15675. El usuario es pedidos360 y la clave es la del .env. No publicar capturas que muestren contrasenas.

5. Crear las imagenes y arrancar las cuatro aplicaciones en Docker:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Stack.ps1 -Apps
```

Esta primera construccion descarga Maven, JDK y dependencias y puede tardar varios minutos. Los tests que necesitan infraestructura se verifican por separado; el Dockerfile empaqueta con `-DskipTests`, sin afirmar que las pruebas se ejecutaron durante el build.

## Puertos y datos aislados

| Componente | Puerto del stack nuevo |
|---|---:|
| Catalogo | 8091 |
| Pedidos | 8092 |
| Consumidores | 8093 |
| Administrador | 8094 |
| AMQP nodo 1 | 45674 |
| AMQP nodo 2 | 45675 |
| Panel nodo 1 | 15674 |
| Panel nodo 2 | 15675 |

El proyecto Compose se llama `pedidos360-stack`; tiene nuevos volumenes y nuevas bases. No modifica los contenedores ni los datos de `pedidos360` que ya funcionaban. Por eso el stack nuevo inicia sin productos ni pedidos: la coleccion los crea. Los servicios Docker se conectan usando nombres DNS internos y el puerto AMQP 5672, no los puertos publicados de Windows.

Puede mantenerse el entorno anterior en paralelo si hay memoria suficiente. Para liberarla, detener las cuatro terminales originales con Ctrl+C y ejecutar `docker compose stop` usando compose.yml. No ejecutar `down -v`: eliminaria datos.

## Verificacion funcional

Importar `postman/Pedidos360_STACK.postman_collection.json` y ejecutarlo en orden con demora 500 ms. Es la misma cobertura funcional de 49 peticiones, apuntando a 8091-8094. La API registra los mensajes confirmados y consumidores escriben resultados en el volumen task_data:

- EMAIL: notificacion preparada, sin envio SMTP.
- KITCHEN: ticket de preparacion de cocina.
- INVOICE: comprobante interno de texto, sin validez tributaria; no es un PDF.

Los comandos ahora exigen el JSON del envelope emitido por orders-service. Una cadena de texto arbitraria publicada manualmente en el panel es invalida y se enviara a DLQ tras el reintento.

El ACK se envia despues de persistir el resultado. Si se entrega otra vez el mismo eventId y contenido, se reconoce el archivo existente. Si el mismo ID trae contenido distinto, falla. Esta deduplicacion local de archivos no es una transaccion distribuida ni garantiza exactly-once entre replicas.

Para ver los resultados y logs:

```powershell
docker compose -f compose.stack.yml exec notify-service sh -c 'find /app/data/tareas -type f'
docker compose -f compose.stack.yml logs --tail 80 notify-service
docker compose -f compose.stack.yml exec rabbit1 rabbitmqctl cluster_status
```

Para extraer los archivos, consultar el ID del contenedor y usar docker cp:

```powershell
$notifyId = docker compose -f compose.stack.yml ps -q notify-service
docker cp "${notifyId}:/app/data/tareas" .\resultados-tareas
```

## Metricas y politicas

La topologia se importa en el cluster mediante la API de administracion despues de detectar los dos miembros. No se incluyen usuarios ni hashes de contrasena en definitions.json.

| Politica | Colas | TTL | Limite |
|---|---|---:|---:|
| cmd-retention | Tres principales | 1 hora | 10000 Ready, reject-publish |
| dlq-retention | Tres DLQ | 7 dias | 10000 Ready |

En principales, overflow reject-publish permite que el productor detecte rechazo por limite. En DLQ, el comportamiento por defecto al superar longitud descarta los mensajes mas antiguos. Estos limites son configurables; no equivalen a retencion infinita.

Abrir Management → Admin → Policies para mostrarlas, y Queues and Streams para Ready, Unacked y Consumers. Para observar Ready, detener notify-service y publicar una tarea, luego reiniciarlo y comprobar ACK/resultado. No afirmar que Unacked tiene que ser mayor a cero: tareas rapidas pueden dejarlo en cero entre refrescos.

```powershell
docker compose -f compose.stack.yml stop notify-service
# Publicar un comando desde Postman y observar Ready.
docker compose -f compose.stack.yml start notify-service
```

Guardar evidencias reales, sin credenciales ni exportaciones de usuarios:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Save-Evidence.ps1 -Apps
```

Genera nodes.json, queues.json, policies.json, exchanges.json y health.json en evidence/ con fecha local. Si no hay exactamente dos nodos activos o faltan colas/politicas, el script falla en lugar de declarar exito.

## Despliegue cloud preparado, aun pendiente de ejecutar

El mismo Docker Compose puede ejecutarse en una VM Linux con Docker Engine y Compose v2 instalados. Esto prepara un despliegue, no demuestra que ya exista en AWS.

1. Tener acceso a la cuenta y a una instancia EC2 con espacio y memoria para las aplicaciones y brokers. Un laboratorio de este tamano necesita mas recursos que una VM minima; dimensionar y revisar el costo antes de crearla.
2. Transferir los fuentes del paquete, Dockerfile, compose.stack.yml e infra; crear .env en la VM con sus claves. No subir claves SSH, .env ni cookies a GitHub.
3. En la VM, ejecutar desde la raiz:

```bash
docker compose -f compose.stack.yml up -d --wait rabbit1 rabbit2
docker compose -f compose.stack.yml run --rm --no-deps rabbit-setup
docker compose -f compose.stack.yml --profile apps up -d --build
docker compose -f compose.stack.yml exec rabbit1 rabbitmqctl cluster_status
```

4. El compose enlaza las APIs y paneles a 127.0.0.1. Para la demostracion desde el equipo local usar un tunel SSH; no hace falta exponer las bases, AMQP ni los puertos de clustering en Internet:

```powershell
ssh -i RUTA_CLAVE.pem -L 8091:127.0.0.1:8091 -L 8092:127.0.0.1:8092 -L 8093:127.0.0.1:8093 -L 8094:127.0.0.1:8094 -L 15674:127.0.0.1:15674 -L 15675:127.0.0.1:15675 USUARIO@IP_PUBLICA
```

Detener antes el stack local si ocupa esos puertos. Seleccionar usuario segun la imagen Linux usada. En el video mostrar tambien la instancia/terminal cloud y sus comandos para demostrar que el procesamiento corre alli; el tunel por si solo no acredita cloud.

Dos contenedores RabbitMQ en una VM forman dos nodos logicos; no son dos VMs ni protegen de perder la VM. Si el docente exige dos maquinas distintas, hace falta adaptar DNS/red privada y configuracion al despliegue de dos hosts.

## Limites que siguen pendientes

- No hay un despliegue cloud verificado ni video grabado por esta ampliacion.
- No se incorporan Angular, Entra ID, JWT, API Gateway ni RDS.
- Pedidos siguen en CREADO y descuento de stock por separado; publicar un comando no representa aceptar un pedido completo.
- Dos nodos cumplen el numero solicitado en el laboratorio; no se presentan como una arquitectura de alta disponibilidad de produccion. Las colas classic de esta topologia no replican automaticamente sus mensajes a otro nodo. El cluster comparte metadatos, y las aplicaciones conocen ambas direcciones, sin prometer continuidad ante la caida de un nodo.

Documentacion de referencia: https://www.rabbitmq.com/docs/4.2/cluster-formation ; https://www.rabbitmq.com/docs/4.2/clustering ; https://www.rabbitmq.com/docs/4.2/http-api-reference ; https://www.rabbitmq.com/docs/ttl ; https://www.rabbitmq.com/docs/maxlength .
