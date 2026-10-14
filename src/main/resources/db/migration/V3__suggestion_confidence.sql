-- The model's self-reported confidence in its suggestion. Nullable so rows written before this
-- migration stay valid. ai_suggestions stays insert-only: the trigger blocks UPDATE and DELETE only.
ALTER TABLE ai_suggestions
    ADD COLUMN confidence TEXT CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH'));
