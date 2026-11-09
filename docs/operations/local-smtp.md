# Local SMTP capture and issue #39

## Choose external delivery or local capture

The API's aggregate `/actuator/health` includes SMTP. Readiness excludes mail;
readiness HTTP 200 therefore does not establish SMTP health. Keep the mail health
indicator enabled. `EmailService` validates its username and password before either
send method; no application code change is needed for a local authenticated SMTP sink.

For external delivery, supply `spring.mail.username` and `spring.mail.password`
through private configuration (or `BOSSANALYZE_SMTP_USERNAME` and
`BOSSANALYZE_SMTP_PASSWORD`). Use the provider's SMTP authorization credential and
required port/TLS settings. Preserve authentication and certificate verification;
never reuse the local plaintext settings below for a provider. Do not rotate existing
credentials or send to real recipients as part of local diagnosis.

For local development only, [Mailpit's official image](https://mailpit.axllent.org/docs/install/docker/)
captures mail without delivering it. Its [SMTP authentication settings](https://mailpit.axllent.org/docs/configuration/smtp/)
can require synthetic credentials over a loopback-only plaintext connection. This
checks application mail construction, SMTP authentication and capture, not external delivery.

## Explicit local opt-in

Check that TCP ports 11025 and 18025 and the container name are unused. Start only
a new container; leave existing services running. The example uses Docker's existing
bridge so an exhausted automatic network pool does not require changing other networks.

```sh
docker run -d --name bossanalyze-mailpit-issue39 \
  --network bridge \
  -p 127.0.0.1:11025:1025 -p 127.0.0.1:18025:8025 \
  -e MP_SMTP_AUTH=local-sender@example.invalid:synthetic-local-only \
  -e MP_SMTP_AUTH_ALLOW_INSECURE=true \
  -e MP_SMTP_DISABLE_RDNS=true -e MP_DISABLE_VERSION_CHECK=true \
  axllent/mailpit:v1.27.4
```

Do not configure SMTP relay, forwarding, webhooks or message release. Reverse DNS
is disabled to avoid local SMTP greeting delays. Messages live in this container's
temporary database and are lost when the container is removed; use synthetic data only.
The UI/API is available at `http://127.0.0.1:18025`.

Create a **new** private properties file with mode 0600, without replacing any
existing file. All credentials in this example are synthetic:

```properties
spring.config.activate.on-profile=local-dev
spring.mail.host=127.0.0.1
spring.mail.port=11025
spring.mail.protocol=smtp
spring.mail.username=local-sender@example.invalid
spring.mail.password=synthetic-local-only
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.properties.mail.smtp.starttls.required=false
spring.mail.properties.mail.smtp.ssl.enable=false
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

At the next authorized API deployment, activate `local-dev` and append the new file
**last** in the existing `--spring.config.additional-location` comma-separated list.
Retain every existing infrastructure/storage location and any other startup options.
Do not replace that list or the underlying private configuration. For example:

```text
--spring.profiles.active=local-dev
--spring.config.additional-location=file:/private/application-local.properties,file:/private/application-store.properties,file:/private/application-mailpit.properties
```

The activation condition makes the overlay inactive outside `local-dev`. Explicit
higher-priority command-line mail properties can still override it: verify the resolved
target before sending. Keep this overlay out of production deployments entirely.

After deployment, check `/actuator/health` and readiness separately. Verify mail health
and, if authorized, send only a synthetic `example.invalid` message through
`EmailService`; confirm its sender, recipient, content and authenticated username in
Mailpit. Do not use the registration endpoint for this check: it can modify Redis.
For rollback, remove only the last mail overlay location on the next authorized
restart. Original configuration and provider authentication then apply again.
Stop/remove only the owned Mailpit container after its consumer has switched away.

## Local evidence: 2026-11-09 UTC

Issue [#39](https://github.com/DavidHLP/BossAnalyze/issues/39) was reproduced:
existing API PID 1445378 returned aggregate HTTP 503 while readiness returned HTTP
200. Its private runtime log contained `AuthenticationFailedException` with
`failed to connect, no password specified`. The specified private base properties
file had no SMTP credential keys. This establishes a missing resolved password for
that runtime, not a provider rejection of a valid authorization code. No credential
was printed, copied or changed; no external message was sent.

An isolated Spring mail context loaded the tracked API YAML, `local-dev` and the
private overlay. The actual `MailHealthIndicator` returned UP and the production
`EmailService.sendSimpleMail` captured exactly one synthetic message. Mailpit recorded
`local-sender@example.invalid` as the authenticated username. A separate context
using the `production` profile ignored the overlay and retained `smtp.163.com` and
SMTP authentication. The existing sensitive configuration/service contracts passed:
32 tests, zero failures/errors/skips, using Java 17 and Maven 3.9.16.

The owned container uses network `bossanalyze-smtp-issue39` (10.254.39.0/24) because
this host's automatic Docker address pool was exhausted. No existing network was
changed. An internal network did not publish ports on this host; the final owned
network is a normal bridge, with loopback-only published ports and no relay configured.

The running API was not restarted or reconfigured. Aggregate health after loading the
overlay into the full API remains for the parent deployment to verify. External
provider authorization, external delivery and production health remain unverified.
