# Revision de entrega EP3

Se revisaron los archivos fuente, configuraciones, Maven Wrapper y poms del ZIP recibido, y se contrastaron con los ocho indicadores del alcance minimo de EP3. El codigo contiene evidencia de los ocho indicadores descritos en la tabla del README. Esto no garantiza una calificacion ni sustituye la revision del docente.

## Hallazgos y cambios

1. El compose recibido exponia RabbitMQ en 5672, mientras los tres servicios AMQP usaban 45672. Se corrigio compose a `127.0.0.1:45672:5672`, conservando el puerto 15672 del panel.
2. No habia .gitignore en la raiz. Se agregaron exclusiones de .env, target, caches y archivos locales.
3. No se incluyo una coleccion Postman en el ZIP. Se preparo una coleccion nueva desde las rutas reales, con variables, captura de IDs y comprobaciones de respuesta. No contiene respuestas de una ejecucion realizada durante esta revision.
4. No habia README de ejecucion. Se agregaron arranque, puertos, pruebas, correspondencia con indicadores y limites funcionales.
5. Se agregaron scripts PowerShell para cargar .env sin evaluar su contenido como codigo, comprobar JDK 21, arrancar infraestructura, iniciar cada servicio y ejecutar clean verify. Los caches de Maven quedan dentro de .cache en el disco del proyecto.
6. Se excluyeron los target compilados del paquete final. Se conservaron los fuentes y Wrapper originales; no se cambio la logica Java que el autor ya habia probado.

## Verificacion y evidencia

- El autor informo cuatro BUILD SUCCESS y pruebas manuales de Postman correctas antes de enviar este ZIP.
- Se revisaron las declaraciones de topologia, consumidores, confirmaciones del productor, inspeccion de DLQ, API administradora y validaciones.
- Se comprobaron estructura XML/JSON/YAML, coincidencia de puertos, integridad del ZIP y presencia de los archivos requeridos.
- Los cambios de configuracion y la coleccion incluida requieren una ultima ejecucion local siguiendo README. En el entorno de revision no hay Docker ni JDK 21/Maven disponibles, por lo que no se volvieron a ejecutar Spring Boot ni las peticiones HTTP. Los scripts PowerShell tampoco se ejecutaron en este entorno Linux.

## Observaciones de alcance

El flujo de negocio esta limitado a pedidos CREADO y comandos explicitos sobre esos pedidos. Los consumidores son demostraciones con logs. La configuracion incluye un solo nodo RabbitMQ local. Estas limitaciones se documentan y no se presentan como funcionalidades completas ni como evidencia cloud de EP4.

Antes de entregar: conservar el .env local fuera de Git, volver a ejecutar Verify.ps1 y la coleccion Postman, subir el repositorio y comprobar su acceso. Revisar con el docente cualquier exigencia acumulativa de EP1/EP2 que exceda los ocho indicadores especificos de RabbitMQ.

## Ampliacion posterior para el bloque 2

Ver AVANCE_BLOQUE_2.md: se incorporaron compose.stack.yml, Dockerfile, politicas, scripts de evidencia, una coleccion para puertos 8091-8094 y TareaService con resultados persistentes. Este cambio SI modifica el consumidor y agrega pruebas; requiere clean verify y pruebas de arranque locales. La descripcion anterior corresponde al primer paquete revisado, previo a esta ampliacion. No se afirma que exista un despliegue cloud.
