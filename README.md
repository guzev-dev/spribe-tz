# About project
**Used technologies:** Java 21, Spring Boot (Data JPA, Web), PostgreSQL 17, Liquibase, JUnit5, Mockito, Docker, OpenAPI, Lombok.

**Exchange rates provider:** [fixer.io](fixer.io)

Receiving of exchange rates scheduled based on `currencyRate.fetch.frequency` property values (*in seconds*).

Exchange rates for currencies where none of pair is the base currency (*EUR for fixer.io*) calculated based on a double conversion basis.

*(Since free plan for most public available source doesn't support changing base currency)*