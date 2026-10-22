# Estado real de requisitos

| Requisito | Estado de esta version | Evidencia pendiente |
|---|---|---|
| Configuracion y beans RabbitMQ | Codigo presente y revisado | Repetir prueba integral tras ampliacion |
| Productor, routing y confirmacion broker | Existente, probado previamente por el autor | Coleccion STACK y logs con el cluster nuevo |
| Tres consumidores por dominio | Codigo presente; ampliados con archivos persistentes | Arranque Maven/Docker de esta revision |
| ACK/NACK y errores | Pruebas aisladas con Channel simulado correctas | Repetir flujo con broker real |
| Tres principales y tres DLQ | Configuracion Java y definitions.json presentes | Captura y Save-Evidence del cluster activo |
| Exchanges direct y topic | Configurados | Prueba en cluster y evidencia |
| Administrador REST y validaciones | Existente, probado previamente por el autor | Repetir coleccion STACK |
| Dos nodos en un cluster | Compose y descubrimiento automatico preparados | cluster_status con ambos nodos activos |
| TTL y max-length | Politicas preparadas para principales y DLQ | Confirmar importacion y mostrar Admin/Policies |
| Metricas Ready, Unacked, Consumers, DLQ | Management y script de captura preparados | Ejecutar Save-Evidence y observar durante prueba |
| Aplicaciones en Docker | Dockerfile y perfil apps preparados | Build real y cuatro health UP |
| Despliegue en cloud | Procedimiento documentado | Instancia, ejecucion e integracion reales |
| Angular, Entra ID, JWT, API Gateway y BD cloud heredados | No implementados | Acordar con docente recuperacion de etapa previa |
| Flujo de aceptacion con stock integrado | No implementado; stock y comandos separados | Integracion y tratamiento de fallos de negocio |
| GitHub accesible | Instrucciones incluidas | Push y comprobacion de acceso del docente |
| Video y defensa EP4 | Pendiente | Evidencias reales y video de 5-10 minutos |

Codigo preparado o prueba simulada no equivalen a despliegue demostrado. No marcar filas cloud como completas por tener archivos de configuracion.
