-- Allow reopening LOST leads into active stages for existing tenant pipelines.
INSERT INTO sales_pipeline_transition (id, pipeline_id, from_stage, to_stage)
SELECT gen_random_uuid(), p.id, 'LOST', 'QUALIFIED'
FROM sales_pipeline p
WHERE NOT EXISTS (
    SELECT 1 FROM sales_pipeline_transition t
    WHERE t.pipeline_id = p.id AND t.from_stage = 'LOST' AND t.to_stage = 'QUALIFIED'
);

INSERT INTO sales_pipeline_transition (id, pipeline_id, from_stage, to_stage)
SELECT gen_random_uuid(), p.id, 'LOST', 'CONTACTED'
FROM sales_pipeline p
WHERE NOT EXISTS (
    SELECT 1 FROM sales_pipeline_transition t
    WHERE t.pipeline_id = p.id AND t.from_stage = 'LOST' AND t.to_stage = 'CONTACTED'
);
