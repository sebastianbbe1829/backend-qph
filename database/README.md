# Database setup

The scripts target the existing PostgreSQL schema `qph_prueba`.

## Existing database upgrade

Run these scripts in order:

1. `users.sql` creates the user table and inserts the existing demo accounts with BCrypt hashes.
2. `migrate_sales_to_multi_item.sql` migrates existing sale rows into sale headers and sale items. Old sales keep a null seller because their original seller was not recorded; new sales record the authenticated user.

The migration keeps the original table as `sales_legacy` for reference. Back up the database before applying it.

## New database

Create the `products_db` database from the first statement in `product_db.sql` if needed, connect to that database, then run the schema and product-table statements from that script. Finally run `users.sql` and the new `sales.sql` once each.

Passwords are never stored as plain text in these scripts. New accounts are saved with BCrypt hashes by the user creation endpoint.
