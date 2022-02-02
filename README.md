# Prerequisites

IntelliJ IDEA 2021.3.1 (Community Edition) used as IDE.

Oracle JDK 1.8.0_321 used after checking in _build.gradle_ that source code compatibility is set to 1.8.

The project is using Spring Boot 1.5.4.RELEASE and no gradle wrapper included on it:
1. [Spring Boot 1.5.X.RELEASE DOCS](https://docs.spring.io/spring-boot/docs/1.5.x/reference/htmlsingle/#getting-started-system-requirements) states that compatible gradle versions are [2.9, 3.x]
2. After downloading Gradle 3.5.1 a wrapper is generated with _gradle wrapper_ and then included into the project
3. After that the project builds successfully using _gradlew build_ and no errors are shown in the IDE
4. Project starts properly on port 18080 as configured _application.yml_

springdoc-openapi-ui:1.3.9 dependency has been included to automatically generate the implemented API documentation (including a swagger-ui interface) to be able to easily test the available endpoints. A proper configuration is also added in _application.yaml_ to redirect http://localhost:18080/ to the swagger-ui web page.

# Development

A new POST endpoint has been implemented in _AccountController_ to perform money transfers between accounts. The transfer information is expected in the RequestBody with the format implemented in the Transfer POJO, that already includes some of the requested validations. Notification sending to account owners and some other validations are performed in _AccountService_, while the transfer itself is done in _AccountRepositoryInMemory_ using thread-safe methods available in the ConcurrentHashMap class.

Regarding tests, they have been implemented for all layers (Controller, Service and Repository) according to provided business rules, including some that checks there are no concurrency issues in the implementation.

# Improvements

- DB: The provided in-memory approach is not useful for a real app. A real DB will not only provide true persistence, but also transactionality.
- Scalability: New features should be properly structured in packages and modules providing decoupling between them.
- CI/CD: Jenkins would be useful to implement CI/CD, discovering branches, building them and deploying the app. Also SonarQube could be integrated to perform code quality analysis.
- Operations audit: Relevant actions could be logged using a dedicated database table for audit in case it is needed.
- Containers: The app could be packaged and deployed using Docker containers to provide extra isolation from the deployment environment.

# Gradle build results
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
