CREATE TABLE taxonomy (
       taxonomy_id               INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       taxonomy_created          TIMESTAMP NOT NULL,
       taxonomy_updated          TIMESTAMP NOT NULL,
       taxonomy_class            VARCHAR(255),
       taxonomy_order            VARCHAR(255),
       taxonomy_family           VARCHAR(255),
       taxonomy_genus            VARCHAR(255),
       taxonomy_species          VARCHAR(255) NOT NULL,
       taxonomy_common_name      VARCHAR(255) NOT NULL,
       taxonomy_notes            LONG VARCHAR,
       PRIMARY KEY (taxonomy_id))
--;;
ALTER TABLE sighting ADD COLUMN taxonomy_id INTEGER REFERENCES taxonomy ON DELETE CASCADE ON UPDATE RESTRICT
