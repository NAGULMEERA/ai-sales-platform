-- Optional per-page Graph token override (global token remains in config/env).
ALTER TABLE meta_page_binding
    ADD COLUMN page_access_token VARCHAR(512);

COMMENT ON COLUMN meta_page_binding.page_access_token IS
    'Optional Meta page access token override; prefer env/global token in prod secrets';
