# Offline access-log geolocation

Analytics uses `com.maxmind.db:maxmind-db:3.1.1` with an optional DB-IP Lite City MMDB. No API key, network lookup or runtime download is used. Java 17 remains the runtime; AI code and scheduling are unchanged.

## Database and lifecycle

1. Obtain the monthly City **MMDB** from [DB-IP](https://db-ip.com/db/download/ip-to-city-lite), accepting its [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) terms. CSV and Country-only databases do not provide the same city/location fields.
2. Store it outside the repository in a private directory readable by the analytics service identity. Mount it read-only. Set `GEOLOCATION_MMDB_PATH` (or `geolocation.mmdb-path`) to its absolute path. Never commit the database or private configuration.
3. Empty path disables enrichment. Missing/unreadable/invalid database warns once at initialization and leaves the service usable. A lookup I/O/format failure disables the reader and emits a diagnostic containing the exception class, without visitor IPs or database contents. `IpGeoService.getStatus()` distinguishes unconfigured, unavailable, ready, lookup-failed and closed.
4. The reader loads into memory and closes with the Spring context. Lookups and close are synchronized. Replace the external file atomically and arrange a controlled analytics restart to load a new release; there is no watcher, downloader or in-process update endpoint. Account for database memory in the service limit.

Unknown country/city remain empty strings for the existing DTO; missing coordinates remain null/absent. No default `(0,0)` is generated. Literal IPv4/IPv6 only: no hostnames, DNS, ports or zone identifiers. Private, loopback, link-local, multicast, documentation and reserved ranges are not geolocated. IP locations are approximate, not device location or identity evidence. Historical proxy/loopback logs cannot be repaired by adding this database.

Country visits sum known-country counts; uniqueVisitors counts distinct IP entries, not people. Percentages use **all** visits including unknown locations as denominator. Valid coordinate pairs are grouped by latitude, longitude and location label with summed visits; a country can contribute without coordinates. `countryCode` remains empty because the retained IpInfo DTO has no ISO-code field. Unknown countries are excluded rather than invented. Existing totalVisits/uniqueVisitors remain unaffected.

The dashboard contains the required [IP Geolocation by DB-IP](https://db-ip.com) link. Keep that attribution on any future page using these results. The current dashboard receives the enriched snapshot but does not add a new map or country chart.

## Explicit proxy boundary

Default: trust no forwarding headers. The Nitro middleware removes `Forwarded`, `X-Real-IP`, `X-Forwarded-*` and incoming `X-Boss-Client-IP`, then sets `X-Boss-Client-IP` from the direct socket peer. Authentication headers are preserved. Existing same-origin proxy paths and targets remain unchanged.

For a verified browser → Nuxt → API topology:

- Bind/restrict API ingress to the intended Nuxt host/network. Set API `security.client-ip.trusted-proxies` (environment `SECURITY_CLIENT_IP_TRUSTED_PROXIES`) to an exact comma-separated list of Nuxt socket addresses as observed by API. No CIDR, wildcard or automatic private-network trust. Include IPv4-mapped or IPv6 spellings only if actually observed.
- Keep Spring `server.forward-headers-strategy=none` so `getRemoteAddr()` is the direct socket peer, not another header-derived value. No generic forwarded-header filter/valve may rewrite it before this check.
- With an additional trusted edge before Nuxt, set Nitro `BOSS_TRUSTED_PROXY_IPS` to its exact socket addresses. That edge must **overwrite** X-Forwarded-For with one validated client literal, remove client-supplied forwarding headers and restrict direct access to Nuxt. Multiple-value XFF is rejected. Other topologies require a separately verified configuration, not a broader allowlist.
- API uses only the dedicated header, only from an explicitly allowed peer, and only for access logging. It does not derive permissions or identity from any client IP header. The same-host peer allowlist assumes that local processes are trusted; use network/service isolation if that is false.

Before enabling trust, verify direct spoofing, edge overwrite, socket address spelling, and logs with synthetic requests. Deployment topology was not verified during this isolated implementation; the default therefore leaves proxy IPs unchanged until configured. Do not enable generic XFF handling as a workaround.

## Verification and rollback

Use Java 17 and Maven 3.9.6+. In this checkout:

```sh
mvn -f backend/pom.xml -pl analytics,api -am verify
mvn -f backend/pom.xml -pl analytics -am test -Dtest=IpGeoServiceTest,GeoStatsTest,LogHealthSecurityTest -Dsurefire.failIfNoSpecifiedTests=false -Dgeo.validation.mmdb=/absolute/private/dbip-city-lite.mmdb
cd frontend
pnpm type-check
pnpm test
pnpm build
```

The optional official-file test is skipped unless its system property is supplied. Other tests use synthetic data/mock readers and isolated Spring contexts without infrastructure, business records or existing application ports. The official-file test checks known public IPv4/IPv6 lookups using the actual reader. This is distinct from a full application startup, Spark nonempty analysis, or deployed browser/proxy acceptance, which belong to integrated validation.

Rollback enrichment by removing the path and restarting under the normal service procedure; raw access totals remain. Remove proxy allowlists to return to socket-only attribution. No existing logs, databases, caches or stored statistics are rewritten by these configuration changes. Cached snapshots may retain earlier results until the normal analysis refresh; do not flush existing caches.
