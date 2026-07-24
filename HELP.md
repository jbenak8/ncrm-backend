# Read Me First
The following was discovered as part of building this project:

* The original package name 'cz.jbenak.ncrm-backend' is invalid and this project uses 'cz.jbenak.ncrm_backend' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.0/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.0/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.0/reference/web/servlet.html)
* [Spring Session for JDBC](https://docs.spring.io/spring-session/reference/)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html)
* [OAuth2 Client](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html#web.security.oauth2.client)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Liquibase Migration](https://docs.spring.io/spring-boot/4.1.0/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase)
* [Java Mail Sender](https://docs.spring.io/spring-boot/4.1.0/reference/io/email.html)
* [Quartz Scheduler](https://docs.spring.io/spring-boot/4.1.0/reference/io/quartz.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.1.0/reference/actuator/index.html)
* [Anthropic Claude](https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html)
* [OpenAI](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
* [PDF Document Reader](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_pdf_page)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

