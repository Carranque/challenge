#Consideraciones y configuración previa

El IDE utilizado ha sido IntelliJ IDEA 2021.3.1 (Community Edition)

Se hace uso del JDK de Oracle en su versión 1.8.0_321 tras comprobar en el fichero _build.gradle_ que la compatibilidad del código fuente se ha establecido a 1.8.

Se comprueba que no se incluye un gradle wrapper en el proyecto y que se ha configurado Spring Boot en su versión 1.5.4.RELEASE.
1. Tras validar las release notes de dicha versión de Spring en [Spring Boot 1.5.X.RELEASE DOCS](https://docs.spring.io/spring-boot/docs/1.5.x/reference/htmlsingle/#getting-started-system-requirements) se comprueba que las versiones de Gradle compatibles son [2.9, 3.x]
2. Se descargan los binarios de Gradle en su version 3.5.1 y se genera un wrapper con el comando _gradle wrapper_ que posteriormente se incluye en el proyecto
3. Tras esta configuración el proyecto base compila y los tests proporcionados ejecutan correctamente desde consola con _gradlew build_ y no se muestra ningún error en el IDE
4. Por medio del IDE se arranca correctamente la aplicación y el servidor tomcat levanta correctamente en el puerto 18080 tal y como está configurado en el fichero _application.yml_

Aunque se sugiere el uso de una aplicación externa (postman) para invocar los endpoints de la aplicación, se incluye en _build.gradle_ la dependencia springdoc-openapi-ui:1.3.9 (última versión compatible con el proyecto proporcionado según pruebas realizadas) para generar automáticamente la documentación de la API implementada. Así mismo, se añade en el fichero _application.yaml_ la configuración adecuada para establecer la url de swagger-ui como página principal, de forma que al acceder a http://localhost:18080/ se muestre directamente y se pueda hacer uso de los endpoints sin necesidad de ninguna herramienta adicional.

NOTA: Además de _accounts-controller_ aparece en swagger-ui un _basic-error-controller_ no relevante al proyecto como parte de la generación automática tras el escaneo de las librerias incluidas en el proyecto. Con configuración adicional es posible hacer que no se muestre, sin embargo por simplicidad y al estar fuera del alcance de la prueba se ha mantenido.

#Consideraciones sobre el desarrollo de la funcionalidad solicitada

Se implementa un nuevo endpoint POST en _AccountController_ para poder realizar transferencias, que espera la información de la transferencia en el RequestBody con la estructura implementada en el POJO Transfer, sobre el que se realizan algunas de las validaciones indicadas. El resto de las validaciones así como el envío de las notificaciones al originante y destinatario de la trasferencia se realizan en _AccountService_, así como la llamada a _AccountRepositoryInMemory_ que realiza la transferencia empleando métodos thread-safe sobre la estructura de datos utilizada para almacenar la información de las cuentas (ConcurrentHashMap).

En cuanto a los tests, se han implementado a nivel de todas las capas (Controller, Service, Repository) de acuerdo a las reglas de negocio indicadas, incluyendo un test para validar que la transferencia funciona correctamente en entornos concurrentes, usando la librería sugerida Thread Weaver.

#Mejoras y cambios para convertir la aplicación en un producto entregable

- BBDD: Con un modelo de datos en memoria como el proporcionado la aplicación no es útil. Es necesario proporcionar una BBDD, que además de persistencia real, proporcionará transaccionalidad, detalle importante para las transferencias que constan de dos operaciones, retirada de saldo de una cuenta e incremento en otra, lo que debería ser una operación atómica (o se realizan ambas o ninguna).
- Escalabilidad: Mantener la aplicación correctamente estructurada, agrupando la funcionalidad en módulos y manteniendolos lo más desacoplados posibles, pudiendo incluso desplegarlos de forma independiente, haciendo uso de una BBDD común.
- Integración continua: El código fuente debería estar adecuadamente versionado, y a través de Jenkins se podrían implementar los pipelines adecuados para descubrir cambios en el repositorio o nuevas ramas y generar, compilar y desplegar automáticamente la aplicación. Jenkins podría integrarse también con herramientas de análisis de la calidad del código como SonarQube.
- Auditoría de operaciones: Operaciones relevantes como las transferencias de saldo deberían quedar registradas en detalle para poder trazarlas en caso que sea necesario. Ampliando el modelo de datos e incluyendo alguna tabla especifica para ello podría ser una solución.
- Uso de contenedores: La aplicación podría ser desplegada en contenedores Docker, lo que permitiría ejecutarla facilmente en diferentes entornos según las necesidades.

#Resultado gradle build antes de la entrega
```
E:\Carranque\Desktop\challenge>gradlew build
:compileJava UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:findMainClass
:jar
:bootRepackage
:assemble
:compileTestJava UP-TO-DATE
:processTestResources NO-SOURCE
:testClasses UP-TO-DATE
:test
2022-01-27 00:58:04.382  INFO 8316 --- [       Thread-6] o.s.w.c.s.GenericWebApplicationContext   : Closing org.springframework.web.context.support.GenericWebApplicationContext@9acca55: startup date [Thu Jan 27 00:57:57 CET 2022]; root of context hierarchy
2022-01-27 00:58:04.384  INFO 8316 --- [      Thread-10] o.s.w.c.s.GenericWebApplicationContext   : Closing org.springframework.web.context.support.GenericWebApplicationContext@4b407099: startup date [Thu Jan 27 00:58:03 CET 2022]; root of context hierarchy
2022-01-27 00:58:04.384  INFO 8316 --- [       Thread-8] o.s.w.c.s.GenericWebApplicationContext   : Closing org.springframework.web.context.support.GenericWebApplicationContext@7e54857e: startup date [Thu Jan 27 00:58:01 CET 2022]; root of context hierarchy
:check
:build

BUILD SUCCESSFUL

Total time: 12.18 secs
```
_**Gabriel Carranque**_