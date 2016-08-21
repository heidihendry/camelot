CREATE TABLE species_mass (
       species_mass_id  INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       species_mass_start INT NOT NULL,
       species_mass_end   INT NOT NULL,
       PRIMARY KEY (species_mass_id))
--;;
INSERT INTO species_mass (species_mass_start, species_mass_end) VALUES (0, 10),(10, 100),(100, 1000),(1000, 10000)
--;;
ALTER TABLE taxonomy ADD COLUMN species_mass_id INTEGER REFERENCES species_mass ON DELETE CASCADE ON UPDATE RESTRICT
