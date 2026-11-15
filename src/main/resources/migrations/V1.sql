-- Ownership comes from LuckPerms permissions, so only the equipped choice per slot is stored.
-- Rows are kept when a rank is lost, so the selection comes back if the rank is regained.
-- slot and cosmetic_id are free-form keys from cosmetics.yml; adding a slot needs no migration.
CREATE TABLE IF NOT EXISTS cosmetic_selections (
    player_uuid UUID NOT NULL,
    slot VARCHAR(32) NOT NULL,
    cosmetic_id VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (player_uuid, slot)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
