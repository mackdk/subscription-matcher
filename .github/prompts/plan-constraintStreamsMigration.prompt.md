# Migration Plan: Drools DRL scoring → Constraint Streams → Timefold Solver

**Repository:** `subscription-matcher`
**Author of plan:** planning session, 2026-09-23
**Audience:** human developer OR agentic implementor. Every step is intended to be executable without further clarification.

---

## 0. Executive summary

The optimization phase currently computes its score with a legacy Drools DRL file
(`src/main/resources/com/suse/matcher/optimization/rules/Scores.drl`) executed by OptaPlanner's
`DrlScoreDirectorFactory`. We want to move to Constraint Streams (CS), and eventually to
Timefold Solver (the maintained fork of OptaPlanner).

The blocker identified up front is the use of `insertLogical(...)` for `Penalty` /
`OneTwoPenalty` / `ConfirmedMatchInPenaltyGroup` facts. CS has no equivalent of logical insertion.
**It is fully replaceable**, but only if the *implicit* semantics of `insertLogical` (equality-based
deduplication plus truth maintenance) are reproduced explicitly. Those semantics are documented
exhaustively in section 2 — that section is the normative specification for the rewrite.

The work is split into three phases:

| Phase | Goal | Can be released independently |
|---|---|---|
| **Phase 0** | Test enhancements that make the migration verifiable | Yes (no behaviour change) |
| **Phase 1** | Replace `Scores.drl` with a `ConstraintProvider`, staying on OptaPlanner 10.2.0 | Yes |
| **Phase 2** | Migrate OptaPlanner 10.2.0 → Timefold Solver | Yes |

Phase 1 and Phase 2 must not be combined. Phase 1 is the only point at which the old and new
scoring implementations can be automatically cross-validated against each other (see §3.2);
Timefold has removed DRL scoring entirely, so that safety net disappears in Phase 2.

### Non-goals

- The **deduction phase** is out of scope. `com.suse.matcher.deduction.Drools` runs a plain Drools
  `KieSession` that is completely independent of the solver. `drools-engine-classic` stays in
  `pom.xml` forever. Do not touch any file under `src/main/resources/com/suse/matcher/deduction/rules/`
  except where §4.2 explicitly says so.
- No change to the JSON input/output contract (`JsonInput` / `JsonOutput`), which is consumed by
  SUSE Manager. Do not add fields to `JsonOutput`.
- No change to move selectors, construction heuristic, local search config, or termination.
- No performance *optimization* work. Performance *parity* is a requirement; improvements are not.

---

## 1. Current architecture (facts an implementor needs)

### 1.1 Pipeline

`src/main/java/com/suse/matcher/Matcher.java`:

1. L44 `baseFacts = FactConverter.convertToFacts(input)` — builds input facts:
   `Timestamp`, `System`, `HostGuest`, `InstalledProduct`, `VirtualizationGroupMember`,
   `Product`, `Subscription`, `SubscriptionProduct`, `PinnedMatch`.
2. L47–48 `drools = new Drools(baseFacts); deducedFacts = drools.getResult();` — deduction phase
   adds `PotentialMatch`, `CentGroup`, `PenaltyGroup`, `HostedProduct`, `GroupInInheritedVirtualization`, …
3. L56 `matches = getMatches(deducedFacts)` — one `Match` per distinct `PotentialMatch.groupId`.
4. L59 `conflictMap = getConflictMap(deducedFacts)`.
5. L62 `sortedPotentialMatches = getPotentialMatches(deducedFacts).sorted().distinct().collect(...)`.
6. L65–66 `new Assignment(matches, deducedFacts, conflictMap, sortedPotentialMatches)` —
   **the entire `deducedFacts` collection becomes `Assignment.problemFacts`**.
7. L69–70 `new OptaPlanner(assignment, testing).getResult()`.
8. L73 `MessageCollector.addMessages(result)`.

### 1.2 Planning model

- `@PlanningSolution` → `com.suse.matcher.optimization.Assignment`
  - `@PlanningScore HardSoftScore score`
  - `@ProblemFactCollectionProperty Collection<Object> problemFacts`
  - `@PlanningEntityCollectionProperty List<Match> getMatches()`
  - `@ValueRangeProvider(id = "booleanRange") List<Boolean> getBooleans()` → `[FALSE, TRUE]`
- `@PlanningEntity` → `com.suse.matcher.optimization.Match`
  - `int getId()`
  - `@PlanningVariable Boolean confirmed`; `boolean isConfirmed()` is null-safe
    (`BooleanUtils.isTrue(confirmed)`); `void setConfirmed(boolean)`

### 1.3 Exact accessor signatures (use verbatim in method references)

```
PotentialMatch : long getSystemId(), long getProductId(), long getSubscriptionId(),
                 int getCentGroupId(), void setCentGroupId(int), int getGroupId()
InstalledProduct: long getSystemId(), long getProductId()
PinnedMatch     : long getSystemId(), long getSubscriptionId()
CentGroup       : int getId(), int getCents()
PenaltyGroup    : int getId(), Long getGuestId()      // NOTE: boxed Long
Subscription    : Long getId(), Integer getQuantity(), Policy getPolicy(), boolean isIgnored()
Policy enum     : PHYSICAL_ONLY, UNLIMITED_VIRTUALIZATION, ONE_TWO, INSTANCE, INHERITED_VIRTUALIZATION
Match           : int getId(), boolean isConfirmed(), void setConfirmed(boolean)
```

Beware of boxed/primitive mismatches in `Joiners.equal(...)`: `PenaltyGroup.getGuestId()` returns
`Long` while `PotentialMatch.getSystemId()` returns `long`. Both sides of a joiner must produce the
same type or indexing will silently never match. Always map explicitly, e.g.
`equal((m, pm, sub) -> pm.getSystemId(), pg -> pg.getGuestId().longValue())`.

### 1.4 Solver configuration

`src/main/resources/com/suse/matcher/optimization/solver-config.xml`:
- `environmentMode REPRODUCIBLE`, `randomType MERSENNE_TWISTER`, `randomSeed 0`
- `<scoreDirectorFactory><scoreDrl>com/suse/matcher/optimization/rules/Scores.drl</scoreDrl></scoreDirectorFactory>`
- construction heuristic: queued entity placer, ORIGINAL order, `ConflictMatchMoveFilter`
- local search: union of `MatchMoveIteratorFactory` + `MatchSwapMoveIteratorFactory`,
  move tabu 50, accepted count limit 5000, termination 1000 unimproved / 15000 steps / 1 hour

`OptaPlanner.java` overrides for tests (`testing == true`):
`EnvironmentMode.FULL_ASSERT` and `unimprovedStepCountLimit = 12`.

### 1.5 Test suite as-is

- `src/test/java/com/suse/matcher/MatcherScenariosTest.java` — 50 parameterized end-to-end
  scenarios under `src/test/resources/com/suse/matcher/scenarios/<N>/`, each with
  `input.json`, `output.json`, `README.md`, `message_report.csv`, `subscription_report.csv`,
  `unmatched_product_report.csv`. Asserts JSON output fields and CSV contents (order-insensitive).
- `ScenarioValidator` caps inputs at ≤100 systems / products / subscriptions.
- `ScenarioSelector` annotation allows running a subset manually.
- Other tests (`CommandLineArgumentsTest`, `RecordTypeAdapterFactoryTest`, `LoggingBootstrapTest`)
  are unrelated to scoring.
- **The score is never asserted anywhere.**

---

## 2. NORMATIVE SPECIFICATION of the current scoring semantics

Everything in this section must be preserved bit-for-bit by the rewrite. Any deviation is a
behaviour change and must be explicitly approved, not silently introduced.

### 2.1 What `insertLogical` actually means here

`insertLogical` performs equality-based deduplication: inserting a fact that `.equals()` an
already-logically-inserted fact does not create a second fact. Therefore:

**(a) `ConfirmedMatchInPenaltyGroup`** — `equals` on `(subscriptionId, penaltyGroupId, guestId)`.
Rule `calculate12Penalties` fires once per confirmed `PotentialMatch`, but the resulting set
contains **distinct `(subscription, penaltyGroup, guest)` triples**. Consequence: a guest covered by
the same subscription on three different products counts **once**, not three times.
→ CS equivalent: `countDistinct(guestId)` grouped by `(subscriptionId, penaltyGroupId)`.
→ A naive `count()` is WRONG.

**(b) `Penalty` from `penaltyForExceededMatches`** — `equals` on
`(subscriptionId, penaltyGroupId, cents)` where `penaltyGroupId` is set to `-centGroupId`.
The rule fires once per confirmed `PotentialMatch`, but the resulting set contains one penalty per
**distinct `(subscription, centGroup)` pair**. This deduplication *is* the bundling discount: N
matches sharing one `CentGroup` are charged once.
→ CS equivalent: `groupBy(subscriptionId, centGroupId)` then sum the cent groups' cents.
→ Charging per confirmed `PotentialMatch` is WRONG.

**(c) `OneTwoPenalty`** extends `Penalty` and adds nothing. No rule matches on the subtype;
`dontExceedSubscriptionCount` matches the supertype `Penalty`. Its only purpose is the debug logging
in `OptaPlanner.logOneTwoPenalties`. It carries **zero scoring semantics**.

**(d) Known latent defect (intentional behaviour change).** `Penalty.equals` uses
`o instanceof Penalty`, not a concrete-class check. Therefore a `OneTwoPenalty(sub, pg, 50)` and a
`Penalty(sub, -centGroupId, cents)` collapse into a single fact when
`pg == -centGroupId && cents == 50`. `Drools.generateId` returns non-negative sequential ids, so
`-centGroupId <= 0` while penalty group ids are normally `>= 0`; the collision is reachable only
when `centGroupId == 0` and the penalty group id is `0` and cents is `50`, or when an input-provided
`virtualizationGroupId` is negative. The CS rewrite does not reproduce this collapse.
**Decision: do not reproduce it. It is a bug fix.** If a scenario's golden score changes because of
it, review and re-baseline explicitly, documenting it in the commit message.

### 2.2 Score definition (target semantics, stated implementation-independently)

Let `C` = set of `Match` facts with `confirmed == true`.
Let `PM(g)` = set of `PotentialMatch` with `groupId == g`.
Let `S` = set of non-ignored `Subscription`.

**Hard constraint `dontExceedSubscriptionCount`** — for each `sub ∈ S`:

```
centGroups(sub)  = { pm.centGroupId : pm ∈ PM(g), g ∈ C, pm.subscriptionId == sub.id }   // a SET
usedCents_A(sub) = Σ over cg ∈ centGroups(sub) of CentGroup(cg).cents

// only when sub.policy == ONE_TWO :
guests(sub, pg)  = { pm.systemId : pm ∈ PM(g), g ∈ C, pm.subscriptionId == sub.id,
                     PenaltyGroup(pg).guestId == pm.systemId }                            // a SET
usedCents_B(sub) = 50 × | { pg : |guests(sub, pg)| is odd } |
// otherwise usedCents_B(sub) = 0

usedCents(sub)   = usedCents_A(sub) + usedCents_B(sub)
available(sub)   = (sub.quantity == Integer.MAX_VALUE) ? Integer.MAX_VALUE : sub.quantity * 100

hardScore += (available - usedCents >= 0)
               ? 0
               : -10 + available * 9 / usedCents          // INTEGER division, int arithmetic
```

Notes that MUST be honoured:
- `available * 9 / usedCents` is **`int` arithmetic with truncating division**. Do not widen to
  `long`, do not use floating point. It overflows for `quantity > ~2.38M`; that overflow exists today
  and must be preserved for parity. Record it as a follow-up ticket, do not fix it in this migration.
- Since `usedCents > available >= 0` in the penalizing branch, `available * 9 / usedCents ∈ [0, 9)`,
  so the DRL value is in `(-10, -1]`. When expressed via `penalize(...)` — which *subtracts* the
  weight — the match weight must be the **positive** value `10 - available * 9 / usedCents`.
- Subscriptions with no confirmed matches contribute `0` and may be omitted from the stream.
- `Subscription.getQuantity()` is a boxed `Integer`. The DRL never guards against `null`; assume
  non-null and let an NPE surface as it does today.

**Soft constraint `maximizeCoveredInstallations`** — `+100` for every triple
`(InstalledProduct ip, PotentialMatch pm, Match m)` with
`pm.systemId == ip.systemId && pm.productId == ip.productId && m.id == pm.groupId && m.confirmed`.
Cardinality matters: each distinct triple scores separately.

**Soft constraint `maximizePinnedMatches`** — `+10` for every triple
`(PinnedMatch pin, PotentialMatch pm, Match m)` with
`pm.systemId == pin.systemId && pm.subscriptionId == pin.subscriptionId && m.id == pm.groupId && m.confirmed`.

**Soft constraint `preferBundledMatches`** — `+1` for every ordered quadruple
`(pm1, pm2, m1, m2)` with
`pm1.systemId == pm2.systemId && pm1.centGroupId == pm2.centGroupId`,
`m1.id == pm1.groupId && m1.confirmed`,
`m2.id == pm2.groupId && m2.id != m1.id`.
Note `m2` is **not** required to be confirmed, and in DRL it also matches entities whose planning
variable is still `null`. See §4.4 for how to express this precisely.

### 2.3 Behaviour that is explicitly allowed to change

1. The `Penalty`/`OneTwoPenalty` equality collapse of §2.1(d).
2. Debug log wording for 1-2 penalties.
3. Constraint match counts / justification shapes (not observable in output).

Everything else is a regression.

---

## 3. PHASE 0 — Test enhancements (prerequisite, no behaviour change)

Rationale: the 50 scenarios assert the *assignment*, never the *score*. On inputs capped at 100
entities, many different scoring functions yield the same optimal assignment, so a subtly wrong
port can pass all 50 tests and still misbehave on real data. Additionally the penalizing branch of
`dontExceedSubscriptionCount` is never observable in the output (final solutions always have hard
score 0) — it only steers the search.

Phase 0 must be merged **before** any Phase 1 code lands.

### 3.1 Task 0.1 — Golden score assertions

**Files:** `src/test/java/com/suse/matcher/MatcherScenariosTest.java`,
new resource `src/test/resources/com/suse/matcher/scenarios/<N>/score.txt` (50 files).

- `Assignment.getScore()` returns `HardSoftScore`. Use `HardSoftScore.parseScore(String)` /
  `toString()` for the file format (e.g. `0hard/1234soft`).
- Add to `testScenario(...)`: read `score.txt` for the scenario and assert equality with
  `assignment.getScore()`. Add `score.txt` to the required-resource list in `scenarioExists(...)`
  so a missing file fails fast rather than silently skipping.
- Generate the 50 baseline files by running the suite once with a temporary
  "write actual score" switch, then **review** the values for plausibility before committing.
- Do **not** add the score to `JsonOutput`; that record is part of the public contract.

Acceptance: `mvn test` green; deliberately perturbing any constant in `Scores.drl`
(e.g. `50` → `51`) makes at least one scenario fail.

### 3.2 Task 0.2 — Dual score-director cross-check harness

This is the single highest-value item. `ScoreDirectorFactoryConfig` in OptaPlanner 10.2.0 supports a
nested `assertionScoreDirectorFactory`; it is built with `EnvironmentMode.NON_REPRODUCIBLE` and
requires the outer `environmentMode` to be `FAST_ASSERT` or stricter. Tests already force
`FULL_ASSERT`, so the requirement is satisfied.

**File:** `src/main/java/com/suse/matcher/optimization/OptaPlanner.java`, in `createConfiguration(boolean testing)`.

Implement as a **test-only, temporary** switch (system property, e.g.
`-Dsubscription-matcher.score.crosscheck=true`, default off) that sets:

```java
ScoreDirectorFactoryConfig assertion = new ScoreDirectorFactoryConfig()
    .withScoreDrls("com/suse/matcher/optimization/rules/Scores.drl");
config.getScoreDirectorFactoryConfig().setAssertionScoreDirectorFactory(assertion);
```

In Phase 0 this is a no-op-ish self-check (DRL vs DRL) and only proves the plumbing works. Its real
use is Phase 1 step 4.6, where the primary director is the `ConstraintProvider` and the assertion
director is the DRL: OptaPlanner then verifies every move's score against the DRL implementation and
fails loudly with both scores printed on any divergence.

Deliverable in Phase 0: the switch exists, is documented in the class javadoc, and a CI/local
command is recorded in `README.md` or `RELEASING.md`. **It must be deleted in Phase 1 step 4.8.**

### 3.3 Task 0.3 — Targeted scenarios for the deduplication semantics

Three new scenarios (numbers 51, 52, 53, following the existing directory convention, each with
`README.md`, `input.json`, `output.json`, `score.txt`, and the three CSVs). Each must use a
**tight `quantity`** so that a miscount actually flips a match — otherwise the test proves nothing.

- **Scenario 51 — guest dedup under ONE_TWO.** A `ONE_TWO` subscription covering a guest that has
  **two or more products installed**, inside a penalty group with an **odd** guest count, quantity
  tight. Purpose: distinguishes `countDistinct(guestId)` from `count()`. Existing scenarios 15 and
  33 cover odd guest counts but appear not to combine that with multiple products per guest —
  verify before writing, and if an existing scenario already covers it, document that instead of
  adding a new one.
- **Scenario 52 — cent group dedup.** Several confirmed `PotentialMatch`es sharing one `CentGroup`
  with **non-zero** cents (hard-bundle shape, cf. scenarios 23–28/37/43), with quantity tight
  enough that charging per match instead of per cent group would drop a match.
- **Scenario 53 — guest in two penalty groups.** A guest that is a member of more than one
  virtualization group, so `PenaltyGroup` facts exist with two different ids for the same
  `guestId`. This scenario decides whether the enrichment of §4.2 needs a `Set<Integer>` or a
  single `int`. If the deduction phase turns out to make multi-membership impossible, record that
  finding in the scenario README and instead add an assertion/comment documenting the invariant.

Also **verify** that at least one existing scenario exercises `quantity == Integer.MAX_VALUE`
(unlimited). If none does, add it as scenario 54.

### 3.4 Task 0.4 — Identify tie-ambiguous scenarios

Scenarios 5 and 6 state in their READMEs that "any of the two subscriptions may be used". Their
expected output is an arbitrary tie-break, stable only because of `randomSeed 0` +
`REPRODUCIBLE`. Swapping the score director should not change move selection, but if one of these
flips it is **not necessarily a regression**.

Deliverable: a list (in the scenario READMEs themselves, one added line each) marking which
scenarios have multiple optimal solutions. Optionally relax those assertions to score + coverage
rather than exact subscription ids. This is documentation work, not a code change — its value is
saving the implementor from chasing a non-bug.

### 3.5 Task 0.5 — Performance baseline

No large-input benchmark exists and the scenarios are capped at 100 entities, yet the Phase 1
design (§4.3) collects a list per subscription and recomputes on each group change, which is
coarser than the current incremental `Penalty` facts.

Deliverable: a recorded baseline (wall clock, plus `Solver` move-evaluation count from the INFO
logs) on at least one large anonymized input, produced with the existing tooling
(`utils/anonymize_scenarios.sh`, `utils/analyze_input_json.rb`). This is a manual harness, not a
unit test; check the measurement procedure and the numbers into `doc/` or the PR description.

**Gate:** Phase 1 may not be declared complete if the same input regresses by more than an agreed
factor (suggested: 1.5×). If it does, escalate to the `concat` design (§5.5) or reconsider.

### 3.6 Task 0.6 — Add the test dependency for ConstraintVerifier

`pom.xml`, test scope, next to `junit-jupiter`:

```xml
<dependency>
    <groupId>org.optaplanner</groupId>
    <artifactId>optaplanner-test</artifactId>
    <version>${kie.version}</version>
    <scope>test</scope>
</dependency>
```

`ConstraintVerifier` only works against a `ConstraintProvider`, so the tests that use it can only be
written in Phase 1 (§4.7). Adding the dependency in Phase 0 keeps the Phase 1 diff focused.

---

## 4. PHASE 1 — OptaPlanner 10.2.0 Constraint Streams

### 4.1 Overview of the constraint-stream design and why it is shaped this way

CS cannot `join` two *derived* streams (joins accept a `Class` or a `forEach` uni stream only), and
OptaPlanner 10.2.0 has **no `concat`** (verified: `concat` exists only in Timefold). The hard
constraint is non-linear in the total used cents, so the ONE_TWO rounding penalty and the cent-group
penalty **cannot** be split into two independent constraints, and they cannot be aggregated
separately and then added.

Consequence: everything must flow through **one `groupBy` keyed on `Subscription`**, and therefore
each tuple entering that `groupBy` must locally know both its cents and its penalty group(s). The
cheapest way to achieve that is to attach both to `PotentialMatch` (§4.2).

In Phase 2, `concat` removes this constraint and the design can be simplified (§5.5).

### 4.2 Task 1.1 — Enrich `PotentialMatch`

**File:** `src/main/java/com/suse/matcher/deduction/facts/PotentialMatch.java`

Add two fields with getters and package/public setters:

```java
private int cents;                       // cents of this match's CentGroup
private Set<Integer> penaltyGroupIds;    // ids of PenaltyGroups whose guestId == systemId; empty if none
```

Rules:
- **Do NOT include the new fields in `equals`, `hashCode`, or `DEFAULT_COMPARATOR`.**
  `Matcher.java` L62 does `.sorted().distinct()` on potential matches and relies on the current
  identity semantics.
- Initialize `penaltyGroupIds` to `Collections.emptySet()` in the constructor so it is never `null`.
- Follow the existing javadoc conventions in the file (every public method documented, constructor
  parameters named with the `In` suffix).

**File:** `src/main/java/com/suse/matcher/Matcher.java`

Populate the new fields in a **post-deduction pass**, after `drools.getResult()` (L48) and before
the `Assignment` is constructed (L65). It MUST NOT be done inside the DRL, because
`mergeCentGroupsInBundles` mutates `PotentialMatch.centGroupId` and `retractOrphanedCentGroups`
retracts orphaned `CentGroup`s during deduction; only the post-deduction state is correct.

```java
// after deduction: every PotentialMatch has exactly one surviving CentGroup
Map<Integer, Integer> centsByCentGroup = CollectionUtils.typeStream(deducedFacts, CentGroup.class)
    .collect(Collectors.toMap(CentGroup::getId, CentGroup::getCents));
Map<Long, Set<Integer>> penaltyGroupsByGuest = CollectionUtils.typeStream(deducedFacts, PenaltyGroup.class)
    .collect(Collectors.groupingBy(pg -> pg.getGuestId(),
             Collectors.mapping(PenaltyGroup::getId, Collectors.toSet())));

CollectionUtils.typeStream(deducedFacts, PotentialMatch.class).forEach(pm -> {
    Integer cents = centsByCentGroup.get(pm.getCentGroupId());
    if (cents == null) {
        throw new IllegalStateException("No CentGroup for PotentialMatch " + pm);
    }
    pm.setCents(cents);
    pm.setPenaltyGroupIds(penaltyGroupsByGuest.getOrDefault(pm.getSystemId(), Set.of()));
});
```

Extract this into a private static helper (e.g. `enrichPotentialMatches(Collection<Object>)`) to keep
`match(...)` readable. The one-line comment above `centsByCentGroup` explains the non-obvious
invariant and should be kept; do not add comments restating the code.

**Note on scenario 53:** if a guest can belong to multiple penalty groups, `Set<Integer>` is
required (the DRL counts such a guest in each of its penalty groups). Keep the `Set` even if
scenario 53 shows multi-membership is currently impossible — it costs nothing and matches the DRL.

### 4.3 Task 1.2 — Create `MatcherConstraintProvider`

**New file:** `src/main/java/com/suse/matcher/optimization/MatcherConstraintProvider.java`

```java
package com.suse.matcher.optimization;

public class MatcherConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
            dontExceedSubscriptionCount(factory),
            maximizeCoveredInstallations(factory),
            maximizePinnedMatches(factory),
            preferBundledMatches(factory)
        };
    }

    private Constraint dontExceedSubscriptionCount(ConstraintFactory factory) {
        return factory.forEach(Match.class)
            .filter(Match::isConfirmed)
            .join(PotentialMatch.class, equal(Match::getId, PotentialMatch::getGroupId))
            .join(Subscription.class, equal((m, pm) -> pm.getSubscriptionId(), sub -> sub.getId().longValue()))
            .filter((m, pm, sub) -> !sub.isIgnored())
            .groupBy((m, pm, sub) -> sub, toList((m, pm, sub) -> pm))
            .filter((sub, pms) -> usedCents(sub, pms) > availableCents(sub))
            .penalize(HardSoftScore.ONE_HARD,
                      (sub, pms) -> 10 - availableCents(sub) * 9 / usedCents(sub, pms))
            .asConstraint("dontExceedSubscriptionCount");
    }

    /** Reproduces the Penalty/OneTwoPenalty facts the DRL used to insert logically. */
    static int usedCents(Subscription subscription, List<PotentialMatch> confirmedMatches) {
        int cents = confirmedMatches.stream()
            .collect(Collectors.toMap(PotentialMatch::getCentGroupId, PotentialMatch::getCents, (a, b) -> a))
            .values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        if (subscription.getPolicy() == Policy.ONE_TWO) {
            Map<Integer, Set<Long>> guestsPerPenaltyGroup = new HashMap<>();
            confirmedMatches.forEach(pm -> pm.getPenaltyGroupIds().forEach(pgId ->
                guestsPerPenaltyGroup.computeIfAbsent(pgId, k -> new HashSet<>()).add(pm.getSystemId())));
            cents += 50 * (int) guestsPerPenaltyGroup.values().stream()
                .filter(guests -> guests.size() % 2 == 1)
                .count();
        }
        return cents;
    }

    static int availableCents(Subscription subscription) {
        return subscription.getQuantity() == Integer.MAX_VALUE
            ? Integer.MAX_VALUE
            : subscription.getQuantity() * 100;
    }
}
```

Implementation requirements:
- `usedCents` and `availableCents` are package-private `static` so §4.7 can unit-test them directly.
- The `filter` before `penalize` calls `usedCents` a second time. That is acceptable and keeps the
  `penalize` lambda total; do not "optimize" it with a mutable cache, which would break
  incrementality assumptions.
- `Collectors.toMap(..., (a, b) -> a)` reproduces §2.1(b): one charge per distinct cent group. All
  potential matches in a cent group carry the same cents value, so the merge function is never
  meaningfully exercised — keep it anyway for safety.
- Static imports: `org.optaplanner.core.api.score.stream.Joiners.equal`,
  `org.optaplanner.core.api.score.stream.ConstraintCollectors.toList`.

### 4.4 Task 1.3 — The three soft constraints

```java
private Constraint maximizeCoveredInstallations(ConstraintFactory factory) {
    return factory.forEach(Match.class)
        .filter(Match::isConfirmed)
        .join(PotentialMatch.class, equal(Match::getId, PotentialMatch::getGroupId))
        .join(InstalledProduct.class,
              equal((m, pm) -> pm.getSystemId(), InstalledProduct::getSystemId),
              equal((m, pm) -> pm.getProductId(), InstalledProduct::getProductId))
        .reward(HardSoftScore.ofSoft(100))
        .asConstraint("maximizeCoveredInstallations");
}

private Constraint maximizePinnedMatches(ConstraintFactory factory) {
    return factory.forEach(Match.class)
        .filter(Match::isConfirmed)
        .join(PotentialMatch.class, equal(Match::getId, PotentialMatch::getGroupId))
        .join(PinnedMatch.class,
              equal((m, pm) -> pm.getSystemId(), PinnedMatch::getSystemId),
              equal((m, pm) -> pm.getSubscriptionId(), PinnedMatch::getSubscriptionId))
        .reward(HardSoftScore.ofSoft(10))
        .asConstraint("maximizePinnedMatches");
}

private Constraint preferBundledMatches(ConstraintFactory factory) {
    return factory.forEach(Match.class)
        .filter(Match::isConfirmed)
        .join(PotentialMatch.class, equal(Match::getId, PotentialMatch::getGroupId))
        .join(PotentialMatch.class,
              equal((m, pm) -> pm.getSystemId(), PotentialMatch::getSystemId),
              equal((m, pm) -> pm.getCentGroupId(), PotentialMatch::getCentGroupId),
              filtering((m, pm, other) -> other.getGroupId() != pm.getGroupId()))
        .reward(HardSoftScore.ofSoft(1))
        .asConstraint("preferBundledMatches");
}
```

**`preferBundledMatches` requires careful review.** The DRL has a fourth pattern
`Match(id == $groupId2, id != $groupId)` which only asserts that *a* `Match` fact exists with
`pm2`'s group id — it does not require it to be confirmed, and it matches uninitialized entities
too. Two options:

- **Option A (recommended).** Every `PotentialMatch.groupId` has a corresponding `Match` by
  construction (`Matcher.getMatches` creates exactly one `Match` per distinct `groupId`). If that
  invariant is confirmed, the fourth pattern is a no-op and the code above is exactly equivalent.
  **Verify the invariant** by reading `Matcher.getMatches(...)` before choosing this option, and
  record the verification in the PR.
- **Option B.** Add an explicit existence check against uninitialized entities too:
  `.ifExists(factory.forEachIncludingNullVars(Match.class), equal(...))`. Note the method name
  differs across versions (`forEachIncludingNullVars` in OptaPlanner 9/10,
  `forEachIncludingUnassigned` in Timefold); check the API of the version in use.

The cross-check harness of §3.2 will detect the difference immediately if Option A's invariant is
false, which is another reason to land that harness first.

Also note: `factory.forEach(Match.class)` excludes entities whose genuine planning variable is
`null`, which matches the DRL's `confirmed == true` intent during the construction heuristic. The
redundant `.filter(Match::isConfirmed)` is kept because `confirmed` can legitimately be `FALSE`.

### 4.5 Task 1.4 — Solver configuration

**File:** `src/main/resources/com/suse/matcher/optimization/solver-config.xml`

Replace:

```xml
<scoreDirectorFactory>
    <scoreDrl>com/suse/matcher/optimization/rules/Scores.drl</scoreDrl>
</scoreDirectorFactory>
```

with:

```xml
<scoreDirectorFactory>
    <constraintProviderClass>com.suse.matcher.optimization.MatcherConstraintProvider</constraintProviderClass>
    <constraintStreamImplType>BAVET</constraintStreamImplType>
</scoreDirectorFactory>
```

**Important:** in the OptaPlanner 9.x/10.x lineage, `ConstraintStreamImplType` may still default to
`DROOLS` (the Drools-backed CS implementation), which would keep the Drools dependency on the
scoring path and has different collector support. Set `BAVET` explicitly. If the element does not
exist in 10.2.0 (it may have been removed when Bavet became the only implementation), drop the line
and record that finding in the PR.

### 4.6 Task 1.5 — Run with the cross-check harness

Enable the Phase 0 switch so the `ConstraintProvider` is primary and `Scores.drl` is the assertion
director, then run the full scenario suite. Every move in all 50+ scenarios is verified against the
DRL implementation. **This must be green before proceeding.** Investigate every divergence against
§2.2 — the DRL is the reference.

Expected legitimate divergence: only the §2.1(d) equality collapse, and only if a scenario happens
to hit it (very unlikely).

### 4.7 Task 1.6 — Constraint unit tests

**New file:** `src/test/java/com/suse/matcher/optimization/MatcherConstraintProviderTest.java`

Using `ConstraintVerifier` from `optaplanner-test`:

```java
private final ConstraintVerifier<MatcherConstraintProvider, Assignment> verifier =
    ConstraintVerifier.build(new MatcherConstraintProvider(), Assignment.class, Match.class);
```

Minimum coverage, with expected values computed **by hand from §2.2**, not by running the code:

1. `maximizeCoveredInstallations` — one confirmed match covering one installed product → `100`;
   an unconfirmed match → `0`; two installed products on one match → `200`.
2. `maximizePinnedMatches` — pinned and confirmed → `10`; pinned but not confirmed → `0`.
3. `preferBundledMatches` — two potential matches sharing system+cent group with different group
   ids, one confirmed → the expected ordered-pair count.
4. `dontExceedSubscriptionCount` — **the important one**, since scenarios cannot reach it:
   - within budget → `0`;
   - one cent group shared by three confirmed matches → charged once (guards §2.1(b));
   - `ONE_TWO` with an odd guest count in a penalty group → `+50`, with an even count → `+0`;
   - `ONE_TWO` guest with two products in the same penalty group → counted **once**
     (guards §2.1(a));
   - exceeded budget → assert the exact integer weight `10 - available * 9 / used`, including a
     case where truncating division matters;
   - `quantity == Integer.MAX_VALUE` → never penalized.
5. Direct tests of `usedCents(...)` and `availableCents(...)` as plain functions.

### 4.8 Task 1.7 — Delete the dead code

Only after §4.6 and §4.7 are green:

- Delete `src/main/resources/com/suse/matcher/optimization/rules/Scores.drl`.
- Delete `src/main/java/com/suse/matcher/optimization/facts/Penalty.java`,
  `OneTwoPenalty.java`, `ConfirmedMatchInPenaltyGroup.java`
  (the whole `com/suse/matcher/optimization/facts/` package becomes empty).
- Remove the cross-check switch added in §3.2 from `OptaPlanner.java`.
- In `OptaPlanner.java`, delete `logOneTwoPenalties(...)` and its imports
  (`DrlScoreDirectorFactory`, `InnerScoreDirectorFactory`, `DefaultSolverFactory`,
  `OneTwoPenalty`, `CollectionUtils`, `Collectors`) and the `instanceof DefaultSolverFactory`
  block in the constructor.
- **Replacement logging.** The 1-2 penalties are a pure function of the solved assignment. If the
  debug output is still wanted, recompute it in plain Java from `result` using the same grouping as
  `usedCents(...)` and log it. Keep it behind `LOGGER.isDebugEnabled()`. Alternatively use
  `ScoreManager`/`ScoreExplanation` constraint-match totals — but since the 1-2 penalty is folded
  into a sum and is not its own constraint, the plain-Java recomputation is simpler and is the
  recommended option.
- Verify `src/main/resources/META-INF/kmodule.xml` — if it declares a kbase/ksession for the
  optimization rules, remove that declaration; leave the deduction declarations untouched.
- `src/main/resources/log4j2.xml` L43–44/49 reference `org.optaplanner` / `org.drools` loggers;
  they stay valid in Phase 1.

Do **not** remove `drools-engine-classic` from `pom.xml` — the deduction phase needs it.
`optaplanner-constraint-drl` is a transitive dependency of `optaplanner-core` and needs no explicit
change.

### 4.9 Phase 1 verification checklist

1. `mvn clean test` — all scenarios green, including the golden scores from §3.1 (unchanged) and
   the new constraint unit tests.
2. Cross-check run (§4.6) green — then the harness is removed.
3. `mvn clean package` produces the shaded jar; smoke-run
   `java -jar target/subscription-matcher-*-with-dependencies.jar` against one scenario input and
   diff the output against the expected `output.json`.
4. Performance re-measured per §3.5 and within the agreed factor of the baseline.
5. `grep -r "Scores.drl\|OneTwoPenalty\|insertLogical" src/` returns nothing under
   `src/main/.../optimization` (`insertLogical` may legitimately remain in deduction rules —
   check before deleting anything there).

---

## 5. PHASE 2 — Migrate to Timefold Solver

### 5.1 Why, and what it costs

OptaPlanner is in maintenance under Apache KIE; Timefold is the actively developed fork. Timefold
has **removed DRL scoring entirely** (its docs list only Constraint Streams, `EasyScoreCalculator`
and `IncrementalScoreCalculator`), which is why Phase 1 must happen first. Timefold also provides
`concat` and `complement`, which allow a cleaner and fully incremental formulation (§5.5).

The migration is mostly mechanical (package renames) plus two areas of genuine risk: the custom
move implementations and the XML config namespace.

### 5.2 Task 2.1 — Dependencies

**File:** `pom.xml`

- Replace `org.optaplanner:optaplanner-core:${kie.version}` with
  `ai.timefold.solver:timefold-solver-core`, and `org.optaplanner:optaplanner-test` with
  `ai.timefold.solver:timefold-solver-test` (test scope).
- Introduce a separate `<timefold.version>` property; **do not** reuse `${kie.version}`, which must
  keep tracking `drools-engine-classic` for the deduction phase.
- Pick the latest stable 1.x release and verify on the release notes that it contains `concat`
  (`concat` is NOT in 1.6.0; it landed later in the 1.x line — confirm against the API of the exact
  version you select, and check `complement` separately, which is newer still).
- Consider importing `ai.timefold.solver:timefold-solver-bom` to avoid version duplication.
- Review the "Transitive dependencies overridden to match SLES shipped version" block: several of
  those pins (`reflections`, `xstream`, `protobuf-java`, `ecj`, `mvel2`, `antlr-runtime`,
  `javassist`) exist because of Drools/OptaPlanner. After the swap, re-run `mvn dependency:tree`
  and remove pins that are no longer reachable; keep the ones the deduction Drools still needs.
  **This must be validated against the SLES packaging constraints before merging** — ask the
  package maintainer if unsure.

### 5.3 Task 2.2 — Package renames

`org.optaplanner.core.api.*` → `ai.timefold.solver.core.api.*`;
`org.optaplanner.core.impl.*` → `ai.timefold.solver.core.impl.*`;
`org.optaplanner.core.config.*` → `ai.timefold.solver.core.config.*`.

Files to change (exhaustive list of `org.optaplanner` references in `src/main` after Phase 1):

- `src/main/java/com/suse/matcher/optimization/Assignment.java` (L6–11)
- `src/main/java/com/suse/matcher/optimization/Match.java` (L10–11)
- `src/main/java/com/suse/matcher/optimization/MatcherConstraintProvider.java` (new in Phase 1)
- `src/main/java/com/suse/matcher/optimization/OptaPlanner.java` (L8–17)
- `src/main/java/com/suse/matcher/optimization/move/ConflictMatchMoveFilter.java` (L6–8)
- `src/main/java/com/suse/matcher/optimization/move/MatchMove.java` (L6–7)
- `src/main/java/com/suse/matcher/optimization/move/MatchMoveIteratorFactory.java` (L5–6)
- `src/main/java/com/suse/matcher/optimization/move/MatchSwapMoveIteratorFactory.java` (L5–6)
- `src/main/java/com/suse/matcher/optimization/move/MatchMoveIterator.java`
- `src/main/java/com/suse/matcher/optimization/move/MatchSwapMoveIterator.java`
- `src/test/java/com/suse/matcher/io/LoggingBootstrapTest.java` (L63, L101 — logger-name assertions)
- `src/test/java/com/suse/matcher/optimization/MatcherConstraintProviderTest.java` (new in Phase 1)
- `src/main/resources/log4j2.xml` (L43–44, L49 — the `org.optaplanner` logger becomes
  `ai.timefold.solver`; the `org.drools` logger stays, deduction still uses Drools)

**Renaming the `OptaPlanner` class itself** (e.g. to `Solver`/`SolverFacade`) is tempting but
touches `Matcher.java` and the `Match` javadoc. Decide once and state it in the PR; the default
recommendation is to rename it to something vendor-neutral in Phase 2 so the codebase does not
carry a misleading name.

### 5.4 Task 2.3 — Configuration and API differences (verify each)

- `solver-config.xml` needs the Timefold namespace:
  `<solver xmlns="https://timefold.ai/xsd/solver" ...>`. Verify the exact schema location against
  the Timefold docs for the chosen version.
- `<constraintStreamImplType>` no longer exists (Bavet only) — remove it.
- The custom `MoveIteratorFactory` / `AbstractMove` APIs are `impl` package APIs and are the most
  likely source of breakage. Check signatures of `AbstractMove.doMoveOnGenuineVariables`,
  `createUndoMove`, `isMoveDoable`, `rebase`, and `MoveIteratorFactory.createRandomMoveIterator`
  against the target version. If they changed, adapt the six classes under
  `src/main/java/com/suse/matcher/optimization/move/`.
- `EnvironmentMode.FULL_ASSERT` / `REPRODUCIBLE`, `randomType MERSENNE_TWISTER`, `randomSeed`,
  `ScoreDirectorFactoryConfig` all exist in Timefold — verify names, some environment modes were
  renamed in later 1.x versions.
- `forEachIncludingNullVars` → `forEachIncludingUnassigned` if Option B of §4.4 was chosen.
- `ConstraintVerifier` moves to `ai.timefold.solver.test.api.score.stream.ConstraintVerifier`.

### 5.5 Task 2.4 — OPTIONAL: simplify using `concat`

Only after §5.6 is green, and only as a separate commit.

With `concat`, the two penalty sources become independent streams that are concatenated before the
per-subscription sum. This removes the need for `toList()` (which Timefold's own docs flag as
defeating incremental calculation) and removes the need for the `PotentialMatch` enrichment of §4.2,
since each branch can join `CentGroup` / `PenaltyGroup` itself.

```java
// one charge per distinct (subscription, centGroup) — replaces the Penalty fact dedup
BiConstraintStream<Long, Integer> centGroupPenalties = factory.forEach(Match.class)
    .filter(Match::isConfirmed)
    .join(PotentialMatch.class, equal(Match::getId, PotentialMatch::getGroupId))
    .join(Subscription.class, equal((m, pm) -> pm.getSubscriptionId(), sub -> sub.getId().longValue()))
    .filter((m, pm, sub) -> !sub.isIgnored())
    .groupBy((m, pm, sub) -> sub.getId(), (m, pm, sub) -> pm.getCentGroupId())
    .join(CentGroup.class, equal((subId, centGroupId) -> centGroupId, CentGroup::getId))
    .map((subId, centGroupId, centGroup) -> subId,
         (subId, centGroupId, centGroup) -> centGroup.getCents());

// 50 cents when a penalty group holds an odd number of covered guests
BiConstraintStream<Long, Integer> oneTwoPenalties = factory.forEach(Match.class)
    .filter(Match::isConfirmed)
    .join(PotentialMatch.class, equal(Match::getId, PotentialMatch::getGroupId))
    .join(Subscription.class, equal((m, pm) -> pm.getSubscriptionId(), sub -> sub.getId().longValue()))
    .filter((m, pm, sub) -> !sub.isIgnored() && sub.getPolicy() == Policy.ONE_TWO)
    .join(PenaltyGroup.class, equal((m, pm, sub) -> pm.getSystemId(), pg -> pg.getGuestId().longValue()))
    .groupBy((m, pm, sub, pg) -> sub.getId(), (m, pm, sub, pg) -> pg.getId(),
             countDistinct((m, pm, sub, pg) -> pm.getSystemId()))
    .filter((subId, penaltyGroupId, guestCount) -> guestCount % 2 == 1)
    .map((subId, penaltyGroupId, guestCount) -> subId,
         (subId, penaltyGroupId, guestCount) -> 50);

return centGroupPenalties.concat(oneTwoPenalties)
    .groupBy((subId, cents) -> subId, sum((subId, cents) -> cents))
    .join(Subscription.class, equal((subId, usedCents) -> subId, sub -> sub.getId().longValue()))
    .filter((subId, usedCents, sub) -> usedCents > availableCents(sub))
    .penalize(HardSoftScore.ONE_HARD,
              (subId, usedCents, sub) -> 10 - availableCents(sub) * 9 / usedCents)
    .asConstraint("dontExceedSubscriptionCount");
```

Notes:
- `concat` deliberately keeps duplicate tuples — which is required here, since two equal-valued
  penalties from different sources must both count.
- The ONE_TWO branch can only produce a tuple for a subscription that also produces a cent-group
  tuple, so no `complement` is needed. Subscriptions with no confirmed matches have zero usage and
  never exceed their budget.
- `countDistinct` on the guest id is §2.1(a) made explicit; the two-key `groupBy` is §2.1(b) made
  explicit.
- Verify `countDistinct` arity support on a Quad stream and `map` to two outputs on a Tri stream in
  the chosen Timefold version.
- If this step is taken, **revert the §4.2 enrichment** (`cents`, `penaltyGroupIds` on
  `PotentialMatch`, and the post-deduction pass in `Matcher.java`) in the same commit, and re-run
  the golden scores. If the golden scores change, the refactor is wrong — the two formulations are
  mathematically identical.

### 5.6 Phase 2 verification checklist

The DRL cross-check no longer exists. Parity is now guarded by:

1. **Golden scores from §3.1 must be byte-identical.** This is the primary oracle. Any change is a
   regression until proven otherwise.
2. All scenario JSON + CSV outputs unchanged, with the §3.4 tie-ambiguous scenarios reviewed
   manually if they differ.
3. Constraint unit tests from §4.7 green after the package rename.
4. `FULL_ASSERT` still enabled in tests — it catches incremental score corruption in the new engine.
5. Performance re-measured per §3.5 against the Phase 1 numbers.
6. `mvn clean package` + shaded-jar smoke run; confirm `META-INF` service files still merge
   correctly (the shade plugin appends `META-INF/kie.conf` — check whether Timefold needs an
   analogous `META-INF/services/...` transformer, since its score director factories are discovered
   via `ServiceLoader`). **This is a real packaging risk: verify the shaded jar actually solves, not
   just that it builds.**
7. `doc/subscription-matcher.1.asciidoc` and `README.md` — update any mention of OptaPlanner/Drools
   scoring.

---

## 6. Ordering, parallelism and gates

```
Phase 0
  0.1 golden scores ─┐
  0.2 cross-check  ──┼─ independent, parallelizable
  0.3 scenarios ─────┤   (0.3 depends on nothing; 0.1 must regenerate baselines if 0.3 adds scenarios,
  0.4 tie-ambiguity ─┤    so land 0.3 BEFORE 0.1, or regenerate)
  0.5 perf baseline ─┤
  0.6 test dep ──────┘
        │
        ▼  GATE: all Phase 0 merged, suite green
Phase 1
  1.1 enrich PotentialMatch
  1.2 ConstraintProvider (hard)      ─ depends on 1.1
  1.3 ConstraintProvider (soft)      ─ parallel with 1.2
  1.4 solver-config                  ─ depends on 1.2, 1.3
  1.5 cross-check run                ─ depends on 1.4        ◄── hard gate
  1.6 constraint unit tests          ─ parallel with 1.5
  1.7 delete dead code               ─ depends on 1.5, 1.6
        │
        ▼  GATE: golden scores unchanged, perf within budget
Phase 2
  2.1 dependencies
  2.2 package renames                ─ depends on 2.1
  2.3 config + impl API fixes        ─ depends on 2.2
  2.4 concat simplification          ─ OPTIONAL, separate commit, after 2.3 is green
```

**Recommended ordering correction:** land 0.3 (new scenarios) *before* 0.1 (golden scores) so the
baselines are generated once for the final scenario set.

---

## 7. Decisions taken, and open questions

### Decided

| # | Decision | Rationale |
|---|---|---|
| D1 | Two separate phases, never combined | Phase 1 is the only point where automatic DRL-vs-CS cross-validation is possible |
| D2 | Enrich `PotentialMatch` in Phase 1 | No `concat` in OptaPlanner 10.2.0 ⇒ single `groupBy` ⇒ tuples must carry cents + penalty groups locally |
| D3 | Enrichment happens post-deduction in `Matcher`, not in DRL | `mergeCentGroupsInBundles` mutates cent group ids and `retractOrphanedCentGroups` retracts facts during deduction |
| D4 | Do not reproduce the `Penalty`/`OneTwoPenalty` equality collapse | It is a latent defect; removing it is a fix, not a regression |
| D5 | Preserve `int` arithmetic and truncating division in the hard penalty, overflow included | Bit-for-bit parity; file a separate follow-up ticket for the overflow |
| D6 | Delete `OneTwoPenalty` and recompute the debug log in plain Java | The type carries no scoring semantics; only `logOneTwoPenalties` reads it |
| D7 | `drools-engine-classic` stays forever | The deduction phase is an independent `KieSession` |
| D8 | Do not add the score to `JsonOutput` | Public contract consumed by SUSE Manager |

### Open — resolve during implementation, record the answer in the PR

| # | Question | How to resolve |
|---|---|---|
| Q1 | Does `<constraintStreamImplType>BAVET</constraintStreamImplType>` exist in OptaPlanner 10.2.0, and what is the default? | Inspect `ScoreDirectorFactoryConfig` in the resolved jar; §4.5 |
| Q2 | Is the "every `PotentialMatch.groupId` has a `Match`" invariant true? | Read `Matcher.getMatches(...)`; decides Option A vs B in §4.4 |
| Q3 | Can a guest belong to more than one penalty group in practice? | Scenario 53 (§3.3); decides `Set<Integer>` vs `int` |
| Q4 | Is there an existing scenario with `quantity == Integer.MAX_VALUE`? | Grep the scenario inputs; add scenario 54 if not |
| Q5 | Which Timefold 1.x version first has `concat` (and `complement`)? | Release notes of candidate versions; NOT in 1.6.0 |
| Q6 | Did the `impl` move APIs change between OptaPlanner 10.2.0 and Timefold? | Compile the six classes under `optimization/move/` against the new jar |
| Q7 | Which SLES-pinned transitive dependencies become unnecessary after the Timefold swap? | `mvn dependency:tree` + confirm with the package maintainer |
| Q8 | Does the shaded jar need a `ServiceLoader` resource transformer for Timefold? | Smoke-run the shaded jar, don't just build it |

---

## 8. Quick reference — files touched

**Phase 0**
- `src/test/java/com/suse/matcher/MatcherScenariosTest.java` (golden scores)
- `src/test/resources/com/suse/matcher/scenarios/<N>/score.txt` (new, ×50+)
- `src/test/resources/com/suse/matcher/scenarios/51..54/**` (new scenarios)
- `src/test/resources/com/suse/matcher/scenarios/5/README.md`, `.../6/README.md` (tie ambiguity)
- `src/main/java/com/suse/matcher/optimization/OptaPlanner.java` (temporary cross-check switch)
- `pom.xml` (`optaplanner-test`)

**Phase 1**
- `src/main/java/com/suse/matcher/deduction/facts/PotentialMatch.java` (+2 fields)
- `src/main/java/com/suse/matcher/Matcher.java` (post-deduction enrichment)
- `src/main/java/com/suse/matcher/optimization/MatcherConstraintProvider.java` (new)
- `src/main/resources/com/suse/matcher/optimization/solver-config.xml`
- `src/test/java/com/suse/matcher/optimization/MatcherConstraintProviderTest.java` (new)
- deleted: `optimization/rules/Scores.drl`, `optimization/facts/{Penalty,OneTwoPenalty,ConfirmedMatchInPenaltyGroup}.java`
- `src/main/java/com/suse/matcher/optimization/OptaPlanner.java` (remove DRL logging + switch)
- `src/main/resources/META-INF/kmodule.xml` (only if it declares the optimization kbase)

**Phase 2**
- `pom.xml` (dependencies)
- all files listed in §5.3
- `src/main/resources/com/suse/matcher/optimization/solver-config.xml` (namespace)
- `src/main/resources/log4j2.xml`
- `README.md`, `doc/subscription-matcher.1.asciidoc`
