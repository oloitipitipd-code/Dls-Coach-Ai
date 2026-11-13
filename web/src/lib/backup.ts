export type BackupPlayer = { id: string; name: string; position: string; rating: number };
export type BackupMatch = { id: string; date: string; gf: number; ga: number };
export type LocalBackup = {
  schemaVersion: 1;
  exportedAt: string;
  players: BackupPlayer[];
  matches: BackupMatch[];
};

const positions = new Set(["GK","CB","LB","RB","CDM","CM","CAM","LM","RM","LW","RW","ST","CF"]);
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);
const isNonEmptyString = (value: unknown): value is string =>
  typeof value === "string" && value.trim().length > 0;

export function createLocalBackup(players: BackupPlayer[], matches: BackupMatch[], exportedAt = new Date().toISOString()): LocalBackup {
  return { schemaVersion: 1, exportedAt, players, matches };
}

/** Strictly validate a backup before changing any locally stored user data. */
export function parseLocalBackup(raw: string): LocalBackup {
  if (raw.length > 5_000_000) throw new Error("Backup is too large (maximum 5 MB).");
  let value: unknown;
  try { value = JSON.parse(raw); } catch { throw new Error("This file is not valid JSON."); }
  if (!isRecord(value) || value.schemaVersion !== 1 || !Array.isArray(value.players) || !Array.isArray(value.matches)) {
    throw new Error("Unsupported or invalid backup format.");
  }
  if (value.players.length > 500 || value.matches.length > 5000) throw new Error("Backup contains too many records.");
  const players: BackupPlayer[] = value.players.map((item, index) => {
    if (!isRecord(item) || !isNonEmptyString(item.id) || !isNonEmptyString(item.name) ||
        !isNonEmptyString(item.position) || !positions.has(item.position) ||
        !Number.isInteger(item.rating) || Number(item.rating) < 1 || Number(item.rating) > 100) {
      throw new Error(`Player record ${index + 1} is invalid.`);
    }
    return { id: item.id, name: item.name.trim(), position: item.position, rating: Number(item.rating) };
  });
  const matches: BackupMatch[] = value.matches.map((item, index) => {
    if (!isRecord(item) || !isNonEmptyString(item.id) || !isNonEmptyString(item.date) ||
        !Number.isFinite(Date.parse(item.date)) || !Number.isInteger(item.gf) || Number(item.gf) < 0 ||
        !Number.isInteger(item.ga) || Number(item.ga) < 0) {
      throw new Error(`Match record ${index + 1} is invalid.`);
    }
    return { id: item.id, date: item.date, gf: Number(item.gf), ga: Number(item.ga) };
  });
  if (new Set(players.map(p => p.id)).size !== players.length ||
      new Set(matches.map(m => m.id)).size !== matches.length) {
    throw new Error("Backup contains duplicate record IDs.");
  }
  return {
    schemaVersion: 1,
    exportedAt: isNonEmptyString(value.exportedAt) ? value.exportedAt : new Date(0).toISOString(),
    players,
    matches,
  };
}
