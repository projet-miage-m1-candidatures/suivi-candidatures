CREATE TABLE utilisateur (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL,
    mot_de_passe_hache TEXT NOT NULL,
    prenom TEXT NOT NULL,
    nom TEXT NOT NULL,
    CONSTRAINT uk_utilisateur_email UNIQUE (email)
);
