-- PostgreSQL only. Local H2 targets V8. Flyway runs this migration in one transaction.
-- Hold writers out between the legacy-data preflight and constraint creation.
LOCK TABLE products IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM products WHERE price < 0) THEN
        -- Fail without echoing product or audit data. Transaction rollback leaves both histories intact.
        RAISE EXCEPTION 'Negative legacy product price; reconcile source history before retrying V10';
    END IF;
END
$$;

ALTER TABLE products
    ADD CONSTRAINT ck_products_price_nonnegative CHECK (price >= 0);
