package com.trungbui.projectshadow.combat;

import com.trungbui.projectshadow.domain.Combatant;
import com.trungbui.projectshadow.domain.CombatEncounter;
import com.trungbui.projectshadow.domain.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

public final class TargetSelector {

    private TargetSelector() {
    }

    public static List<Combatant> candidates(TargetType type, Combatant attacker, CombatEncounter encounter) {
        if (type == TargetType.SELF) return List.of(attacker);
        if (!type.isSupported()) return List.of();

        List<? extends Combatant> foes = foesOf(attacker, encounter);
        List<? extends Combatant> allies = alliesOf(attacker, encounter);
        List<Combatant> aliveFoes = filterAlive(foes);
        List<Combatant> aliveAllies = filterAlive(allies);

        return switch (type) {
            case ANY_FOE -> aliveFoes;
            case BACK_FOE -> aliveFoes.stream().filter(c -> c.position().isBack()).toList();
            case ANY_ALLY -> aliveAllies;
            default -> List.of();
        };
    }

    public static List<Combatant> autoResolve(
            TargetType type,
            Combatant attacker,
            CombatEncounter encounter,
            RandomGenerator rng
    ) {
        if (!type.isSupported()) return List.of();
        if (type == TargetType.SELF) return List.of(attacker);

        List<Combatant> foes = filterAlive(foesOf(attacker, encounter));
        List<Combatant> allies = filterAlive(alliesOf(attacker, encounter));

        return switch (type) {
            case FRONT_FOE -> firstByPosition(foes, true, 1);
            case BACK_FOE -> firstByPosition(foes, false, 1);
            case ANY_FOE -> firstByPosition(foes, true, 1);
            case LOWEST_HP_FOE -> lowestHp(foes, 1);
            case RANDOM_FOE -> random(foes, rng, 1);
            case RANDOM_FOE_2 -> random(foes, rng, 2);
            case FRONT_2_FOE -> firstByPosition(foes, true, 2);
            case FRONT_ROW_FOE -> foes.stream().filter(c -> c.position().isFront()).toList();
            case BACK_ROW_FOE -> foes.stream().filter(c -> c.position().isBack()).toList();
            case ALL_FOE -> List.copyOf(foes);
            case ANY_ALLY -> firstByPosition(allies, true, 1);
            case LOWEST_HP_ALLY -> lowestHp(allies, 1);
            case ALL_ALLY -> List.copyOf(allies);
            default -> List.of();
        };
    }

    public static List<Combatant> resolveWithPick(
            TargetType type,
            Combatant attacker,
            CombatEncounter encounter,
            Combatant picked,
            RandomGenerator rng
    ) {
        if (!type.requiresPlayerPick()) {
            return autoResolve(type, attacker, encounter, rng);
        }
        if (picked == null || !picked.isAlive()) return List.of();
        if (!candidates(type, attacker, encounter).contains(picked)) return List.of();
        return List.of(picked);
    }

    private static List<? extends Combatant> foesOf(Combatant attacker, CombatEncounter encounter) {
        if (attacker instanceof Hero) return encounter.enemies();
        return encounter.heroes();
    }

    private static List<? extends Combatant> alliesOf(Combatant attacker, CombatEncounter encounter) {
        if (attacker instanceof Hero) return encounter.heroes();
        return encounter.enemies();
    }

    private static List<Combatant> filterAlive(List<? extends Combatant> in) {
        List<Combatant> out = new ArrayList<>(in.size());
        for (Combatant c : in) if (c.isAlive()) out.add(c);
        return out;
    }

    private static List<Combatant> firstByPosition(List<Combatant> in, boolean ascending, int n) {
        List<Combatant> sorted = new ArrayList<>(in);
        sorted.sort(Comparator.comparingInt(c -> c.position().rank()));
        if (!ascending) Collections.reverse(sorted);
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    private static List<Combatant> lowestHp(List<Combatant> in, int n) {
        List<Combatant> sorted = new ArrayList<>(in);
        sorted.sort(Comparator.comparingInt(Combatant::currentHp));
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    private static List<Combatant> random(List<Combatant> in, RandomGenerator rng, int n) {
        if (in.isEmpty()) return List.of();
        List<Combatant> shuffled = new ArrayList<>(in);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Combatant tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        return shuffled.subList(0, Math.min(n, shuffled.size()));
    }
}
