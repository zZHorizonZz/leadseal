package dev.horizon.leadseal;

/**
 * A schema after a migration, as its {@code snapshots/<storageHash>/contract.json} file stores it.
 *
 * @param schemaVersion the version of the file format
 * @param storage the schema
 * @param storageHash the hash of the schema
 */
record Snapshot(String schemaVersion, Schema storage, String storageHash) {

  /** The version of the file format that this version of leadseal writes and reads. */
  static final String VERSION = "1";

  Snapshot(Schema storage, String storageHash) {
    this(VERSION, storage, storageHash);
  }
}
