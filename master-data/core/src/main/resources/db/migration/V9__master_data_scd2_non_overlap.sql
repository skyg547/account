-- PostgreSQL only. Local H2 explicitly targets V8; production release migrations include V9.
-- Flyway executes this migration transactionally. Keep all four histories stable during preflight
-- and constraint creation so a concurrent writer cannot invalidate the checked snapshot.
LOCK TABLE account_subjects, business_partners, departments, products IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_extension extension
        JOIN pg_namespace namespace ON namespace.oid = extension.extnamespace
        WHERE extension.extname = 'btree_gist' AND namespace.nspname <> 'public'
    ) THEN
        RAISE EXCEPTION 'SCD2 migration requires btree_gist in public; review the extension schema before retrying';
    END IF;

    -- A running maximum detects nested ranges too; comparing only the preceding row would miss them.
    -- These errors identify the table without echoing business keys or other stored values.
    IF EXISTS (
        SELECT 1 FROM (
            SELECT valid_from, MAX(valid_to) OVER (
                PARTITION BY code ORDER BY valid_from, id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ) AS previous_end FROM account_subjects
        ) history WHERE valid_from <= previous_end
    ) THEN
        RAISE EXCEPTION 'SCD2 overlap in account_subjects; review legacy history and retry without Flyway repair';
    END IF;
    IF EXISTS (
        SELECT 1 FROM (
            SELECT valid_from, MAX(valid_to) OVER (
                PARTITION BY business_partner_code ORDER BY valid_from, id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ) AS previous_end FROM business_partners
        ) history WHERE valid_from <= previous_end
    ) THEN
        RAISE EXCEPTION 'SCD2 overlap in business_partners; review legacy history and retry without Flyway repair';
    END IF;
    IF EXISTS (
        SELECT 1 FROM (
            SELECT valid_from, MAX(valid_to) OVER (
                PARTITION BY code ORDER BY valid_from, id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ) AS previous_end FROM departments
        ) history WHERE valid_from <= previous_end
    ) THEN
        RAISE EXCEPTION 'SCD2 overlap in departments; review legacy history and retry without Flyway repair';
    END IF;
    IF EXISTS (
        SELECT 1 FROM (
            SELECT valid_from, MAX(valid_to) OVER (
                PARTITION BY product_code ORDER BY valid_from, id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ) AS previous_end FROM products
        ) history WHERE valid_from <= previous_end
    ) THEN
        RAISE EXCEPTION 'SCD2 overlap in products; review legacy history and retry without Flyway repair';
    END IF;
END
$$;

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

-- Both boundary dates are included. Adjacent days are allowed; sharing an endpoint is overlap.
-- All history participates, including use_yn=false partners: historical lookup also reads those rows.
-- The explicit public opclass works when the migration connection's search_path is a custom schema.
ALTER TABLE account_subjects
    ADD CONSTRAINT ex_account_subjects_no_overlap EXCLUDE USING gist (
        code public.gist_text_ops WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
    );
ALTER TABLE business_partners
    ADD CONSTRAINT ex_business_partners_no_overlap EXCLUDE USING gist (
        business_partner_code public.gist_text_ops WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
    );
ALTER TABLE departments
    ADD CONSTRAINT ex_departments_no_overlap EXCLUDE USING gist (
        code public.gist_text_ops WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
    );
ALTER TABLE products
    ADD CONSTRAINT ex_products_no_overlap EXCLUDE USING gist (
        product_code public.gist_text_ops WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
    );
