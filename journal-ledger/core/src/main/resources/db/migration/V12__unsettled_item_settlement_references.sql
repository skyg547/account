CREATE TABLE IF NOT EXISTS unsettled_item_settlement_references (
    unsettled_item_id BIGINT NOT NULL,
    settlement_reference VARCHAR(100) NOT NULL,
    PRIMARY KEY (unsettled_item_id, settlement_reference),
    CONSTRAINT fk_unsettled_ref_item
        FOREIGN KEY (unsettled_item_id) REFERENCES unsettled_items (id)
);

INSERT INTO unsettled_item_settlement_references (unsettled_item_id, settlement_reference)
SELECT ui.id, ui.last_settlement_reference
FROM unsettled_items ui
WHERE ui.last_settlement_reference IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM unsettled_item_settlement_references uisr
      WHERE uisr.unsettled_item_id = ui.id
        AND uisr.settlement_reference = ui.last_settlement_reference
  );
