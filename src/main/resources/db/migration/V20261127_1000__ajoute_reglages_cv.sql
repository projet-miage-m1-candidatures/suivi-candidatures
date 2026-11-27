ALTER TABLE cv
    ADD COLUMN couleur VARCHAR(20) NOT NULL DEFAULT 'NOIR'
        CHECK (couleur IN ('NOIR', 'MARINE', 'BLEU', 'BORDEAUX', 'SAPIN', 'ANTHRACITE')),
    ADD COLUMN taille VARCHAR(20) NOT NULL DEFAULT 'NORMALE'
        CHECK (taille IN ('PETITE', 'NORMALE', 'GRANDE')),
    DROP CONSTRAINT cv_modele_check,
    DROP CONSTRAINT cv_police_check,
    ADD CONSTRAINT cv_modele_check CHECK (modele IN ('CLASSIQUE', 'MODERNE', 'COMPACT', 'ELEGANT', 'EPURE')),
    ADD CONSTRAINT cv_police_check CHECK (police IN (
        'INTER', 'LATO', 'IBM_PLEX_SANS', 'SOURCE_SERIF_4', 'LORA', 'CARLITO', 'OPEN_SANS', 'ROBOTO', 'EB_GARAMOND'
    ));

UPDATE cv SET couleur = 'BLEU' WHERE modele = 'MODERNE';
