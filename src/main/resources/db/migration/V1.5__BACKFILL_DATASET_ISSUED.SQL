-- Backfill metadata.issued (ISO date) for dataset embeddings stored before the field existed, parsed from
-- the summary's own "Datasettet ble utgitt dd.MM.yyyy" sentence. The last occurrence is used, since the
-- publisher's description comes first and may contain the same phrase. Rows without a valid date (missing,
-- malformed or impossible such as 31.02) are left without issued instead of failing the migration.
DO $$
DECLARE
    r      RECORD;
    raw    TEXT;
    parsed DATE;
BEGIN
    FOR r IN
        SELECT id, content
        FROM embeddings
        WHERE metadata->>'type' = 'DATASET'
          AND metadata->>'issued' IS NULL
          AND content ~ 'Datasettet ble utgitt \d{2}\.\d{2}\.\d{4}'
    LOOP
        raw := substring(r.content FROM '.*Datasettet ble utgitt (\d{2}\.\d{2}\.\d{4})');
        BEGIN
            parsed := to_date(raw, 'DD.MM.YYYY');
        EXCEPTION WHEN others THEN
            CONTINUE;
        END;
        -- to_date may roll impossible dates over instead of failing; require a round trip
        IF to_char(parsed, 'DD.MM.YYYY') <> raw THEN
            CONTINUE;
        END IF;

        UPDATE embeddings
        SET metadata = metadata || jsonb_build_object('issued', to_char(parsed, 'YYYY-MM-DD'))
        WHERE id = r.id;
    END LOOP;
END $$;
