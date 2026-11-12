export type CoachPlayer = { id: string; name: string; position: string; rating: number };
export type CoachMatch = { id: string; date: string; gf: number; ga: number };

const formations: Record<string, string[]> = {
  "4-2-3-1": ["GK","CB","CB","LB","RB","CDM","CDM","CAM","LW","RW","ST"],
  "4-3-3": ["GK","CB","CB","LB","RB","CDM","CM","CM","LW","RW","ST"],
  "4-1-2-1-2": ["GK","CB","CB","LB","RB","CDM","CM","CM","CAM","ST","ST"],
  "3-5-2": ["GK","CB","CB","CB","LM","RM","CDM","CM","CAM","ST","ST"],
  "5-3-2": ["GK","CB","CB","CB","LB","RB","CDM","CM","CM","ST","ST"],
  "4-4-2": ["GK","CB","CB","LB","RB","LM","RM","CM","CM","ST","ST"],
};
const alternatives: Record<string, string[]> = {
  LB: ["LB","LM"], RB: ["RB","RM"], CDM: ["CDM","CM"], CM: ["CM","CDM","CAM"],
  CAM: ["CAM","CM","CF"], LM: ["LM","LW","LB"], RM: ["RM","RW","RB"],
  LW: ["LW","LM","ST"], RW: ["RW","RM","ST"], ST: ["ST","CF"], CF: ["CF","ST"],
};

function assessFormation(players: CoachPlayer[], slots: string[]) {
  const remaining = [...players];
  let filled = 0;
  let ratingTotal = 0;
  const missing: string[] = [];
  for (const slot of slots) {
    const eligible = remaining
      .map((player, index) => ({ player, index, exact: player.position === slot }))
      .filter(item => item.exact || (alternatives[slot] ?? [slot]).includes(item.player.position))
      .sort((a,b) => Number(b.exact)-Number(a.exact) || b.player.rating-a.player.rating);
    const best = eligible[0];
    if (!best) { missing.push(slot); continue; }
    remaining.splice(best.index, 1);
    filled++;
    ratingTotal += best.player.rating;
  }
  return { filled, averageRating: filled ? ratingTotal / filled : 0, missing };
}

/** Transparent offline rules engine. It is not a generative language model. */
export function generateLocalCoachReport(players: CoachPlayer[], matches: CoachMatch[]): string {
  const lines: string[] = ["DLS ULTIMATE AI — OFFLINE COACH REPORT", "Analysis runs on this device. No Gemini, ChatGPT, cloud AI or internet request is used.", ""];
  if (!players.length) {
    lines.push("SQUAD DATA", "No players have been entered yet.", "Next step: add your goalkeeper, defenders, midfielders and forwards with verified positions and ratings.");
  } else {
    const counts = new Map<string, number>();
    players.forEach(p => counts.set(p.position, (counts.get(p.position) ?? 0) + 1));
    const average = players.reduce((sum,p)=>sum+p.rating,0)/players.length;
    lines.push("SQUAD OVERVIEW", `Registered players: ${players.length}`, `Mean entered overall rating: ${average.toFixed(1)}`, `Goalkeepers: ${counts.get("GK") ?? 0} · Centre-backs: ${counts.get("CB") ?? 0} · Central midfielders (CM/CDM/CAM): ${(counts.get("CM") ?? 0)+(counts.get("CDM") ?? 0)+(counts.get("CAM") ?? 0)} · Strikers: ${(counts.get("ST") ?? 0)+(counts.get("CF") ?? 0)}`);
    const ranked = Object.entries(formations).map(([name,slots])=>({name,...assessFormation(players,slots)})).sort((a,b)=>b.filled-a.filled || b.averageRating-a.averageRating);
    lines.push("", "FORMATION FIT (ROLE-COVERAGE HEURISTIC)");
    ranked.slice(0,3).forEach((f,i)=>lines.push(`${i+1}. ${f.name}: ${f.filled}/11 roles covered; selected-player mean rating ${f.averageRating.toFixed(1)}${f.missing.length ? `; uncovered roles: ${f.missing.join(", ")}` : ""}`));
    const best = ranked[0];
    if (best.filled < 11) lines.push(`Priority: address the ${11-best.filled} uncovered role(s) in ${best.name} before treating it as a complete XI.`);
    if (!(counts.get("GK") ?? 0)) lines.push("Data warning: no goalkeeper is registered. Formation recommendations are incomplete.");
    if ((counts.get("CB") ?? 0) < 2) lines.push("Squad-depth warning: fewer than two centre-backs are registered.");
    if ((counts.get("ST") ?? 0) + (counts.get("CF") ?? 0) < 1) lines.push("Squad-depth warning: no recognised striker is registered.");
    lines.push("Important: this greedy role-fit estimate uses entered positions and ratings only. It does not model hidden game mechanics, chemistry, player movement or predict wins.");
  }
  lines.push("", "MATCH RECORD");
  if (!matches.length) lines.push("No match results have been logged. Add results before drawing conclusions about form.");
  else {
    const recent = matches.slice(0,5);
    const wins = recent.filter(m=>m.gf>m.ga).length;
    const draws = recent.filter(m=>m.gf===m.ga).length;
    const losses = recent.length-wins-draws;
    const scored = recent.reduce((s,m)=>s+m.gf,0);
    const conceded = recent.reduce((s,m)=>s+m.ga,0);
    lines.push(`Latest ${recent.length} logged matches: ${wins} wins, ${draws} draws, ${losses} losses.`, `Goals: ${scored} scored, ${conceded} conceded (mean ${(conceded/recent.length).toFixed(1)} conceded per match).`);
    if (conceded > scored) lines.push("Review defensive transitions and match notes; the logged sample shows more goals conceded than scored.");
    else if (scored > conceded) lines.push("The logged sample shows more goals scored than conceded. Review match footage before attributing this to a particular tactic.");
    else lines.push("The logged sample has an equal goals-for and goals-against total.");
    lines.push("This is descriptive analysis of manually entered results, not proof that a formation caused the outcomes.");
  }
  lines.push("", "DATA QUALITY", "Only user-entered player and match records were analysed. Opponent formation, live events, player attributes not entered, and DLS-specific mechanics are unknown unless separately documented.");
  return lines.join("\n");
}
