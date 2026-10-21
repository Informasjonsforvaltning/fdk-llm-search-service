-- Backfill metadata.issued (ISO date) for dataset embeddings stored before the field existed,
-- parsed from the "ble utgitt dd.MM.yyyy" sentence of the embedded summary.
UPDATE embeddings
SET metadata = metadata || jsonb_build_object(
        'issued',
        to_char(to_date(substring(content FROM 'ble utgitt (\d{2}\.\d{2}\.\d{4})'), 'DD.MM.YYYY'), 'YYYY-MM-DD'))
WHERE metadata->>'type' = 'DATASET'
  AND metadata->>'issued' IS NULL
  AND content ~ 'ble utgitt \d{2}\.\d{2}\.\d{4}';
