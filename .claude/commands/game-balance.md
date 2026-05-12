# Game Balance Analyzer

Analyze Project Shadow's balance data and produce a detailed report with outlier detection and adjustment suggestions.

## Steps

1. **Read CSV headers and data** — read all five balance files:
   - `assets/data/heroes.csv` — columns: Rarity, Hero ID, Role, Position, Base HP, Base DMG Min, Base DMG Max, Base Accuracy, Base Crit, Speed, Base Stress Resist, Default Skills, Available Skills
   - `assets/data/enemies.csv` — columns: Name, Enemy ID, Base HP, Damage Min, Damage Max, Critical Chance, Stress Resist, Accuracy, Variant Type
   - `assets/data/skills.csv` — columns: Skill ID, Hero Class (EN), Is Offensive, Target Type, Damage Multiplier, Accuracy Modifier, Cooldown, Stress Damage, Rarity
   - `assets/data/effects.csv` — columns: Effect ID, Category, Modifier Value, Duration Type, Default Duration
   - `assets/data/items.csv` — inspect header row for drop_chance column

2. **Compute hero metrics** for each hero (group by Role):
   - **DPS score** = average(Damage Multiplier of offensive skills) × (Base Accuracy / 100)
   - **Survivability** = Base HP (positional modifier noted separately)
   - **Stress output** = average(Stress Damage of offensive skills)
   - **Speed** = raw Speed value (turn order advantage)

3. **Compute enemy metrics** (group by Variant Type: common / elite / miniboss / boss):
   - **Threat score** = Base HP × ((Damage Min + Damage Max) / 2) / 100
   - **Hit rate** = Accuracy value
   - **Stress pressure** = any skills with stress_dmg > 0

4. **Outlier detection** — for each metric within its group:
   - Calculate mean and standard deviation
   - Flag any entry where value > mean + 1.5 × SD or < mean − 1.5 × SD
   - Mark severity: HIGH (>2 SD), MEDIUM (1.5–2 SD)

5. **Produce report** in this exact structure:

---

## Project Shadow Balance Report — {date}

### Hero DPS Rankings (all heroes, sorted descending)
| Hero ID | Role | DPS Score | Stress Output | Speed | Flag |
|---------|------|-----------|---------------|-------|------|

### Hero Survivability Rankings (all heroes, sorted descending)
| Hero ID | Role | Base HP | Rarity | Flag |
|---------|------|---------|--------|------|

### Enemy Threat Rankings (grouped by Variant Type)
| Enemy ID | Variant | Threat Score | Accuracy | Flag |
|----------|---------|--------------|----------|------|

### Outliers — Needs Review
List every flagged entry with: ID, metric, value, group mean, deviation level (HIGH/MEDIUM), and impact assessment.

### Suggested Adjustments
For each HIGH outlier, provide a specific suggested change:
- Format: `[hero/enemy ID] [metric] [current value] → [suggested value]` with brief reason
- Keep adjustments minimal (±10–20% from group mean)
- These are suggestions only — human decides what to apply

---

**Important:** Parse CSV rows carefully — skip header row, handle quoted fields. Use actual numeric values only (skip rows where numeric fields are empty or "—").
