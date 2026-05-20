-- Baseline schema. Per-repository tables are created lazily by MysqlRepository at first use.
-- This script only establishes the bookkeeping table; the schema_version table itself is created
-- by MigrationRunner before this script runs.

-- Reserved keyword? Keep it simple — no extra tables needed at the baseline.
SELECT 1;
