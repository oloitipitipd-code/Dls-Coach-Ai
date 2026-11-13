import { describe, expect, it } from "vitest";
import { generateLocalCoachReport, type CoachMatch, type CoachPlayer } from "./localCoach";

describe("generateLocalCoachReport", () => {
  it("explains when no squad data exists and does not claim a formation", () => {
    const report = generateLocalCoachReport([], []);
    expect(report).toContain("No players have been entered yet");
    expect(report).toContain("No match results have been logged");
    expect(report).toContain("No Gemini, ChatGPT, cloud AI or internet request");
  });

  it("ranks formations from entered positions and reports missing roles", () => {
    const players: CoachPlayer[] = [
      { id: "1", name: "Keeper", position: "GK", rating: 80 },
      { id: "2", name: "Centre 1", position: "CB", rating: 82 },
      { id: "3", name: "Centre 2", position: "CB", rating: 81 },
      { id: "4", name: "Left back", position: "LB", rating: 79 },
      { id: "5", name: "Right back", position: "RB", rating: 78 },
      { id: "6", name: "DM", position: "CDM", rating: 84 },
      { id: "7", name: "CM 1", position: "CM", rating: 83 },
      { id: "8", name: "CM 2", position: "CM", rating: 80 },
      { id: "9", name: "Left wing", position: "LW", rating: 85 },
      { id: "10", name: "Right wing", position: "RW", rating: 86 },
      { id: "11", name: "Striker", position: "ST", rating: 90 },
    ];
    const report = generateLocalCoachReport(players, []);
    expect(report).toContain("FORMATION FIT");
    expect(report.includes("4-3-3: 11/11 roles covered") || report.includes("4-2-3-1: 11/11 roles covered")).toBe(true);
    expect(report).toContain("does not model hidden game mechanics");
  });

  it("uses the calendar order, not input order, to select the latest five matches", () => {
    const matches: CoachMatch[] = [
      { id: "oldest", date: "2026-11-01", gf: 0, ga: 9 },
      { id: "2", date: "2026-11-09", gf: 1, ga: 0 },
      { id: "3", date: "2026-11-10", gf: 2, ga: 1 },
      { id: "4", date: "2026-11-11", gf: 0, ga: 2 },
      { id: "5", date: "2026-11-12", gf: 1, ga: 1 },
      { id: "newest", date: "2026-11-13", gf: 3, ga: 0 },
    ];
    const report = generateLocalCoachReport([], matches);
    expect(report).toContain("Latest 5 logged matches: 3 wins, 1 draws, 1 losses.");
    expect(report).toContain("7 scored, 4 conceded");
    expect(report).not.toContain("0 – 9");
  });

  it("does not mutate the caller's match list when sorting records", () => {
    const matches: CoachMatch[] = [
      { id: "later", date: "2026-11-13", gf: 1, ga: 0 },
      { id: "earlier", date: "2026-11-12", gf: 0, ga: 1 },
    ];
    const originalOrder = matches.map(match => match.id);
    generateLocalCoachReport([], matches);
    expect(matches.map(match => match.id)).toEqual(originalOrder);
  });
});
