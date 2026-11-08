# Database initialization snapshots

- `system.sql` defines the `blog_data` system/authentication schema.
- `boss.sql` defines the `boss_data` recruitment schema.

These files contain `DROP TABLE` statements. Import them only into a new, empty
database; never apply them to an existing instance to update a checkout.
This directory move leaves both scripts unchanged and does not require running
either script. They are initialization snapshots, not incremental migrations.

See [local development](../docs/local-development.md) for external connection
configuration. Keep credentials and database exports containing real data out of Git.
