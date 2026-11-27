ALTER TABLE cv
    DROP CONSTRAINT cv_modele_check,
    DROP CONSTRAINT cv_police_check,
    ADD CONSTRAINT cv_modele_check CHECK (modele IN (
        'CLASSIQUE', 'MODERNE', 'COMPACT', 'ELEGANT', 'EPURE', 'CHRONOLOGIQUE'
    )),
    ADD CONSTRAINT cv_police_check CHECK (police IN (
        'INTER', 'LATO', 'IBM_PLEX_SANS', 'SOURCE_SERIF_4', 'LORA', 'CARLITO', 'OPEN_SANS', 'ROBOTO', 'EB_GARAMOND', 'GELASIO'
    ));
