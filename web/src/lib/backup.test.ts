import { describe, expect, it } from "vitest";
import { createLocalBackup, parseLocalBackup } from "./backup";

describe("offline backup", () => {
  const players = [{ id: "p1", name: "Keeper", position: "GK", rating: 82 }];
  const matches = [{ id: "m1", date: "2026-11-13T12:00:00.000Z", gf: 2, ga: 1 }];

  it("round-trips valid local records", () => {
    const backup = createLocalBackup(players, matches, "2026-11-13T12:00:00.000Z");
    expect(parseLocalBackup(JSON.stringify(backup))).toEqual(backup);
  });

  it("rejects unsupported schema versions", () => {
    expect(() => parseLocalBackup(JSON.stringify({ schemaVersion: 2, players: [], matches: [] })))
      .toThrow("Unsupported or invalid backup format");
  });

  it("rejects invalid player ratings and positions", () => {
    const invalidRating = { schemaVersion: 1, players: [{ ...players[0], rating: 101 }], matches: [] };
    expect(() => parseLocalBackup(JSON.stringify(invalidRating))).toThrow("Player record 1 is invalid");
    const invalidPosition = { schemaVersion: 1, players: [{ ...players[0], position: "HACK" }], matches: [] };
    expect(() => parseLocalBackup(JSON.stringify(invalidPosition))).toThrow("Player record 1 is invalid");
  });

  it("rejects negative goals, duplicate IDs and oversized files", () => {
    const invalidMatch = { schemaVersion: 1, players: [], matches: [{ ...matches[0], gf: -1 }] };
    expect(() => parseLocalBackup(JSON.stringify(invalidMatch))).toThrow("Match record 1 is invalid");
    const duplicate = { schemaVersion: 1, players: [players[0], players[0]], matches: [] };
    expect(() => parseLocalBackup(JSON.stringify(duplicate))).toThrow("duplicate record IDs");
    expect(() => parseLocalBackup(" ".repeat(5_000_001))).toThrow("maximum 5 MB");
  });
});
