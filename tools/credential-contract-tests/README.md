# Offline credential contracts

This bounded test project compiles the actual OpenRouter and IP geolocation
production classes with the project's Spring Boot 3.4.5 dependency baseline.
It does not start the full applications, databases, Redis, Spark or Consul.
All credentials are synthetic; environment variables and JVM system properties
are removed from each test Spring environment. HTTP is intercepted by a mock
request factory or a mocked Apache client; no real provider calls are made.

With a complete JDK 17+ and Maven 3.9.6+, prepare official dependencies
and run the stubbed tests once, then repeat from clean outputs offline:

    mvn -f tools/credential-contract-tests/pom.xml test
    mvn -o -f tools/credential-contract-tests/pom.xml clean test

The first command may contact Maven repositories for dependencies and test
providers; its application HTTP is still mocked. The second command uses Maven
offline mode. Mockito is passed as a test JVM agent to avoid dynamic attachment. The tests cover missing/blank configuration,
property-over-environment precedence, lazy OpenRouter initialization, HTTP
headers/response parsing, local IP handling and Redis placeholder resolution.
The Maven module intentionally compiles only the two production classes under
test. Passing it is not a whole-project build, live API test, credential
revocation check or production deployment acceptance.

Redis assertions inspect the actual default and tracked `local-dev` YAML and
exercise both strict placeholder resolution and Spring Boot's Binder. Strict
resolution throws on a missing default variable, but that is **not** the Boot
runtime binding contract: Binder retains the unresolved literal and accepts
explicit empty/whitespace values. No Redis fail-fast guarantee is introduced.
Only the two Java API credentials reject missing/blank values at initialization.

The existing tracked `local-dev` profiles use
`${BOSSANALYZE_REDIS_PASSWORD:}` and preserve passwordless local development when
unset. These files are not changed. Tests verify that behavior and configured
synthetic overrides without opening a Redis connection.
