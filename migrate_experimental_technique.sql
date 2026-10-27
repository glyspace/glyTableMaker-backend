BEGIN;

CREATE TEMP TABLE technique_map (label text PRIMARY KEY, uri text, id text) ON COMMIT DROP;
INSERT INTO technique_map (label, uri, id) VALUES
    ('MS', 'http://purl.obolibrary.org/obo/CHMO_0000470', 'CHMO_0000470'),
    ('MS/MS', 'http://purl.obolibrary.org/obo/CHMO_0000575', 'CHMO_0000575'),
    ('LC-MS/MS', 'http://purl.obolibrary.org/obo/CHMO_0000701', 'CHMO_0000701'),
    ('LC-MS', 'http://purl.obolibrary.org/obo/CHMO_0000524', 'CHMO_0000524'),
    ('CE-MS', 'http://purl.obolibrary.org/obo/CHMO_0000702', 'CHMO_0000702'),
    ('CE', 'http://purl.obolibrary.org/obo/CHMO_0001024', 'CHMO_0001024'),
    ('HPLC', 'http://purl.obolibrary.org/obo/CHMO_00001009', 'CHMO_00001009'),
    ('GC', 'http://purl.obolibrary.org/obo/CHMO_00001002', 'CHMO_00001002'),
    ('GC-MS', 'http://purl.obolibrary.org/obo/CHMO_0000497', 'CHMO_0000497'),
    ('Imaging MS', 'http://purl.obolibrary.org/obo/CHMO_0000053', 'CHMO_0000053'),
    ('IMS-MS', 'http://purl.obolibrary.org/obo/CHMO_0000499', 'CHMO_0000499'),
    ('IMS', 'http://purl.obolibrary.org/obo/CHMO_0000430', 'CHMO_0000430'),
    ('Gel Electrophoresis', 'http://purl.obolibrary.org/obo/CHMO_00001021', 'CHMO_00001021'),
    ('1D NMR', 'http://purl.obolibrary.org/obo/CHMO_0000592', 'CHMO_0000592'),
    ('2D NMR', 'http://purl.obolibrary.org/obo/CHMO_0000598', 'CHMO_0000598'),
    ('1H NMR', 'http://purl.obolibrary.org/obo/CHMO_0000593', 'CHMO_0000593'),
    ('TOCSY', 'http://purl.obolibrary.org/obo/CHMO_0000605', 'CHMO_0000605'),
    ('NOESY', 'http://purl.obolibrary.org/obo/CHMO_0000607', 'CHMO_0000607'),
    ('Solid-state NMR', 'http://purl.obolibrary.org/obo/CHMO_0000614', 'CHMO_0000614'),
    ('X-ray Crystallography', 'http://purl.obolibrary.org/obo/CHMO_0000159', 'CHMO_0000159'),
    ('Cryo-EM', 'http://purl.obolibrary.org/obo/CHMO_0002413', 'CHMO_0002413');

-- ===== collections: collections.metadatavalues =====
-- dry run: rows that will change
SELECT count(*) AS rows_to_change FROM collections t
WHERE jsonb_typeof(t.metadatavalues -> 'experimentalTechnique') = 'array'
  AND EXISTS (SELECT 1 FROM jsonb_array_elements(t.metadatavalues -> 'experimentalTechnique') x
              WHERE jsonb_typeof(x) = 'string');

UPDATE collections t
SET metadatavalues = jsonb_set(
        t.metadatavalues,
        '{experimentalTechnique}',
        (SELECT COALESCE(jsonb_agg(
                    CASE WHEN jsonb_typeof(e.elem) = 'string'
                         THEN jsonb_strip_nulls(jsonb_build_object(
                                  'name', trim(e.elem #>> '{}'),
                                  'uri',  m.uri,
                                  'id',   m.id))      -- free-text ("other") values get only a name
                         ELSE e.elem END
                    ORDER BY e.ord), '[]'::jsonb)
         FROM jsonb_array_elements(t.metadatavalues -> 'experimentalTechnique') WITH ORDINALITY AS e(elem, ord)
         LEFT JOIN technique_map m ON lower(m.label) = lower(trim(e.elem #>> '{}')))
    )
WHERE jsonb_typeof(t.metadatavalues -> 'experimentalTechnique') = 'array'
  AND EXISTS (SELECT 1 FROM jsonb_array_elements(t.metadatavalues -> 'experimentalTechnique') x
              WHERE jsonb_typeof(x) = 'string');

SELECT metadatavalues -> 'experimentalTechnique' AS converted FROM collections
WHERE metadatavalues ? 'experimentalTechnique' LIMIT 10;


-- ===== dataset metadata groups: datasetmetadatagroup.value =====
-- dry run: rows that will change
SELECT count(*) AS rows_to_change FROM datasetmetadatagroup t
WHERE jsonb_typeof(t.value -> 'experimentalTechnique') = 'array'
  AND EXISTS (SELECT 1 FROM jsonb_array_elements(t.value -> 'experimentalTechnique') x
              WHERE jsonb_typeof(x) = 'string');

UPDATE datasetmetadatagroup t
SET value = jsonb_set(
        t.value,
        '{experimentalTechnique}',
        (SELECT COALESCE(jsonb_agg(
                    CASE WHEN jsonb_typeof(e.elem) = 'string'
                         THEN jsonb_strip_nulls(jsonb_build_object(
                                  'name', trim(e.elem #>> '{}'),
                                  'uri',  m.uri,
                                  'id',   m.id))      -- free-text ("other") values get only a name
                         ELSE e.elem END
                    ORDER BY e.ord), '[]'::jsonb)
         FROM jsonb_array_elements(t.value -> 'experimentalTechnique') WITH ORDINALITY AS e(elem, ord)
         LEFT JOIN technique_map m ON lower(m.label) = lower(trim(e.elem #>> '{}')))
    )
WHERE jsonb_typeof(t.value -> 'experimentalTechnique') = 'array'
  AND EXISTS (SELECT 1 FROM jsonb_array_elements(t.value -> 'experimentalTechnique') x
              WHERE jsonb_typeof(x) = 'string');

SELECT value -> 'experimentalTechnique' AS converted FROM datasetmetadatagroup
WHERE value ? 'experimentalTechnique' LIMIT 10;

-- inspect the output above; COMMIT if it looks right, otherwise ROLLBACK
COMMIT;
