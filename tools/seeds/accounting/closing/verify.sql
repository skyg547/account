SELECT to_regclass('closing_calendars') IS NOT NULL
AND to_regclass('valuation_batches') IS NOT NULL
AND to_regclass('provision_batches') IS NOT NULL
AND to_regclass('closing_adjustments') IS NOT NULL
AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
 AND table_name='daily_closing_status' AND column_name='state' AND is_nullable='NO')
AND NOT EXISTS (SELECT 1 FROM daily_closing_status WHERE date='2090-01-15' AND state<>'OPEN')
AND NOT EXISTS (SELECT 1 FROM period_locks WHERE fiscal_period_id=6900001);
