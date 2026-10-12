# Deduction Phase

## Purpose and Phase Boundary

`subscription-matcher` has two distinct responsibilities: finding possible coverage and choosing a combination of coverage options. Deduction performs the first responsibility; it does not decide the final allocation of subscription capacity.

In [Matcher.java](../src/main/java/com/suse/matcher/Matcher.java), input is converted into base facts and passed to the deduction engine. The resulting `PotentialMatch` facts describe candidate coverage. Each distinct candidate group becomes one optimization `Match`, initially unselected. Candidates covering the same system/product pair are also collected into conflict sets.

The [optimization phase](optimization-phase.md) chooses which candidate groups to select. Eligibility does not imply that a candidate can coexist with every other candidate or fit within the purchased subscription quantity.

For a domain-level explanation of subscription policies, candidate coverage, selection groups, and capacity cents, see the [business logic overview](business-logic.md).

## Business Context and Input Responsibilities

Subscription matching produces an allocation report showing which system/product installations use which subscriptions, reported subscription usage, and which installations lack coverage. A subscription can cover several installations, and one system can need several subscriptions for different products or stacked capacity: the overall relationship is not simply one subscription per machine.

The report is an allocation estimate based on supplied inventory, topology, product associations, and catalog rules. It is not a legal audit or a determination of contractual compliance. Missing topology and conservative CPU defaults can change the estimate, and finite optimization may not find the best possible allocation.

### Domain Vocabulary

| Term | Interpretation in the current matcher |
| --- | --- |
| Physical system | A hardware system represented by `System.physical=true`; it may host guests. |
| Virtual guest | A non-physical system, normally linked to a host through `HostGuest` or included in an explicit virtualization group. |
| Virtual host | A system with at least one supplied host/guest relationship. A physical host remains physical; being a host does not change that flag. |
| Virtualization environment | A business grouping of hosts across which guests can move, such as infrastructure managed by a vCenter. `subscription-matcher` does not discover such environments. |
| Virtualization group | A supplied set of guests treated together for ONE_TWO rounding. Groups can represent an environment, cloud, or other upstream grouping. Without explicit membership, guests are grouped by host. |
| Virtual host manager | An upstream source of topology or group membership, not a distinct planning entity or discovery service in this component. |

The broad term "virtual system" can refer to a host or a guest, so it should not be treated as equivalent to `physical=false`. A physical hypervisor hosting guests is still a physical system in the implementation.

[JsonSystem.java](../src/main/java/com/suse/matcher/io/json/JsonSystem.java) includes `virtualHost`, but [FactConverter.java](../src/main/java/com/suse/matcher/deduction/FactConverter.java) does not read that flag. Supplied `virtualSystemIds` create `HostGuest` facts. Consequently, a physical system marked `virtualHost=true` but having no guest relationships is not excluded from ONE_TWO matching merely by that flag.

### Upstream Contract

[JsonInput.java](../src/main/java/com/suse/matcher/io/json/JsonInput.java) is the boundary between inventory/subscription gathering and matching. The caller must supply the timestamp, systems, products, subscriptions, guest topology or [explicit virtualization groups](../src/main/java/com/suse/matcher/io/json/JsonVirtualizationGroup.java), and optional pins.

SCC order-item processing, OEM quantity derivation, subscription partitioning, registration mechanisms, and hypervisor-specific gathering are upstream responsibilities, not deduction rules. In particular:

- OEM quantity must already be populated in input; `subscription-matcher` does not derive it from SCC `system_limit` or order items.
- A partitioned match must receive the intended subset of subscriptions and systems. The input model has no organization or ISS-slave partition field; `sccUsername` is not a general partitioning mechanism.
- Requirements without an installed package, such as synthetic LTSS or system-entitlement products, can be represented by supplied product IDs and system/product associations. `subscription-matcher` does not synthesize those requirements from maintenance-channel or registration state.
- Provider, hypervisor, and architecture-specific discovery is not performed here. The explicit group `type` contributes to group identity, not to a provider-specific eligibility rule.

Supplying a cloud-like virtualization group enables group-based accounting, not an automatic provider-specific exemption or BYOS policy. Likewise, the generic host/guest input does not establish support for nested-VM or container licensing; those cases have no dedicated policy model here.

## Drools Execution Model

[Drools.java](../src/main/java/com/suse/matcher/deduction/Drools.java) creates a fresh stateful `KieSession` for each deduction call, inserts the base facts, and calls `fireAllRules()`. A fact is a Java object known to the rule engine. A rule has a `when` section describing matching facts and a `then` section executed when that combination is found. Newly inserted or modified facts can enable further rules: this is forward chaining.

The agenda is the collection of eligible rule activations. This implementation focuses agenda groups in reverse order because focus behaves like a stack, so the intended execution order is:

1. `PartNumbers`
2. `InputValidation`
3. `InputAugmenting`
4. `SubscriptionAggregation`
5. `HardBundleConversion`
6. `Matchability`

After the rules finish, the engine returns all remaining session objects, including surviving input facts and generated facts. The session is then closed. The `idGenerator` global supplies identifiers; it is a service available to rule consequences (i.e. the rule `then` block), not an input fact matched by rule conditions.

## Reading the Rule Language

The current [pom.xml](../pom.xml) targets Java 17 and uses `drools-engine-classic` and `optaplanner-core` with `kie.version` set to `10.2.0`. The application uses classic DRL (Drools Rule Language), not Constraint Streams. KIE is the shared API for building and running rule knowledge bases.

| Construct | Meaning in this application |
| --- | --- |
| `System($systemId : id, physical == true)` | Find a physical-system fact and bind its ID to a variable. |
| Several patterns in `when` | Join facts satisfying all the conditions, for example a system, its installed product, and a subscription offering that product. |
| `not Pattern(...)` | Require that no matching fact exists. |
| `or` | Allow either branch to supply a matching fact. |
| `accumulate(...; count()/collectSet()/sum())` | Aggregate matching facts into a count, set, or total. |
| `insert(...)` | Add a fact that can activate more rules. |
| `modify(fact) { ... }` | Change a fact and notify Drools, so dependent conditions can be reconsidered. |
| `retract(fact)` | Remove a fact; dependent activations are invalidated. |
| `salience` | Higher values run first within the focused group. |
| `no-loop` | Prevent a rule's own updates from immediately causing it to activate itself again. It is not a general cycle detector. |

The rules declare their groups with `ruleflow-group`; the Java facade establishes their order through agenda-group focus. This ordering is deliberate: matchability must see enriched, validated, normalized subscriptions. File order is not a reliable rule execution order within a group.

[kmodule.xml](../src/main/resources/META-INF/kmodule.xml) limits this deduction knowledge base to `com.suse.matcher.deduction.rules`. It configures equality-based fact handling: equal objects are treated as the same fact, rather than retaining every newly allocated object independently. Consequently, repeated derivations of an equal `HostedProduct` or `CentGroup` need not multiply the facts. Equality is defined by each Java fact class.

Some fact classes are `@PropertyReactive`: Drools tracks relevant property changes rather than assuming every modification affects every rule. For example, `match12Subscription` explicitly binds `physical` so correction of that property can reactivate it.

Deduction uses ordinary insertion. This differs from the optimization score rules' `insertLogical`, whose derived facts are automatically removed when their justifications disappear. See the [optimization guide](optimization-phase.md).

## Base Facts from JSON

[FactConverter.java](../src/main/java/com/suse/matcher/deduction/FactConverter.java) decomposes input records into small, joinable facts:

| Input | Facts inserted | Role |
| --- | --- | --- |
| Input timestamp | `Timestamp` | Reference date for subscription validity. |
| System | `System(id, name, cpus, physical)` | Machine attributes; CPUs mean populated sockets or IFLs in the domain model. |
| System product IDs | `InstalledProduct(systemId, productId)` | Coverage requirements. |
| System guest IDs | `HostGuest(hostId, guestId)` | Host/guest topology. |
| Virtualization group | `VirtualizationGroupMember(groupId, guestId)` | Explicit guest grouping used for ONE_TWO accounting. |
| Product | `Product(id, name, productClass, free, base)` | Product metadata, including base/add-on and bundle distinctions. |
| Subscription | `Subscription` | Purchased quantity, dates, part number, name, and SCC username. |
| Subscription product IDs | `SubscriptionProduct(subscriptionId, productId)` | Products the entitlement can cover. |
| Pinned match | `PinnedMatch(systemId, subscriptionId)` | A preferred allocation, later rewarded by the optimizer. |

Missing product `free` and `base` flags become false. Explicit virtualization group IDs are generated from their input type and ID, separating groups of different types.

CPU entitlement can be expressed in populated sockets, IFLs, or catalog-specific counting units. The input has only one numeric `cpus` field, not separate socket, IFL, and core counts. Some current fixtures, such as [SLE Micro](../src/test/resources/com/suse/matcher/scenarios/45/README.md), describe core-based counts. Callers must align the supplied count with the applicable catalog entitlement; `subscription-matcher` does not independently discover or convert hardware counting units.

**IFL** stands for **Integrated Facility for Linux**, a processor on IBM Z mainframes dedicated to Linux workloads. For applicable subscriptions, IFLs are counted as entitlement units instead of CPU sockets.

[FactIdGenerator.java](../src/main/java/com/suse/matcher/deduction/FactIdGenerator.java) maps a namespace and tuple of values to a sequential integer. The same tuple gives the same ID from one generator instance; different tuples receive different IDs. These IDs coordinate rules, but should not be interpreted as persisted business identifiers or stable IDs across fresh generator instances.

## Important Deduction Rules

### 1. Part Number Enrichment

[PartNumbers.drl](../src/main/resources/com/suse/matcher/deduction/rules/PartNumbers.drl) is a large entitlement catalog. Rules named `Add subscription data for part number ...` match a literal part number and populate:

- `policy`: the physical/virtual coverage model.
- `supportLevel`: a catalog attribute; it is not directly rewarded by current scoring.
- `cpus`: socket/IFL entitlement per unit, where applicable.
- `stackable`: whether units may be combined in relevant policies.
- `singleSubscriptionHardBundle`: whether a subscription already represents a hard bundle.
- `quantity`: some catalog entries convert purchased bundles into matchable units or use `Integer.MAX_VALUE` for effectively unrestricted instance capacity.

For example, part number `051-003964` becomes PHYSICAL_ONLY, basic support, four CPUs, non-stackable; `051-004063` becomes INSTANCE with no CPU limit set; `113-003087-001` becomes stackable UNLIMITED_VIRTUALIZATION with one CPU per unit.

LTSS examples illustrate why the catalog, rather than a generic product-family rule, is authoritative. Part number `874-006660` receives INHERITED_VIRTUALIZATION. Part numbers `874-007222` and `874-007223` receive INSTANCE and multiply input quantity by 100 and 500 respectively; `874-007224` receives INSTANCE with quantity `Integer.MAX_VALUE`. The quantity-changing rules use `no-loop` to avoid repeatedly applying the conversion to their own updates.

Subscription start and end dates determine validity, not a contract-era classification. CPU limits, policy, and stackability come from the part-number rule. Supported coverage must therefore be checked against the actual catalog entry and supplied product associations, rather than inferred from the product family, contract date, or system architecture. The system input has no architecture field.

LTSS requirements must be supplied as product associations. `subscription-matcher` has no maintenance-calendar logic to decide when LTSS is needed or when included SAP access expires. It also has no code-stream field or dedicated LTSS scoring dimension: unlimited-virtualization grouping is keyed by host and subscription, not additionally by code stream. Separate product IDs and subscription entries can affect counting, but do not imply a universal per-code-stream consumption rule.

Unknown part numbers are not inferred from subscription names. For a non-ignored subscription with a non-null, unrecognized part number, the policy remains null and validation emits `unknown_part_number`; it cannot produce coverage candidates.

### 2. Input Validation and Correction

[InputValidation.drl](../src/main/resources/com/suse/matcher/deduction/rules/InputValidation.drl) changes what the later rules can see:

| Rule | Effect |
| --- | --- |
| `ignoreExpiredSubscription` | Sets `ignored=true` if the end date is before the input timestamp or the start date is after it. Equality at the boundary is allowed. |
| `ignoreNotActiveSubscriptions` | Ignores subscriptions with either date missing. |
| `ignoreServiceSubscriptionsWithoutProducts` | Ignores null-policy subscriptions with no associated products. Emits `no_products_associated` unless the part number starts with `051-`. |
| `ignoreSubscriptionWithoutQuantity` | Ignores subscriptions whose quantity is null. This is not a general positive-quantity check. |
| `retractBlacklistedProduct` | Removes installed requirements for product IDs 955, 957, 1232, 1241, and 1242. |
| `retractFreeProduct` | Removes installed requirements for products marked free. |
| `logUnknownPartNumber` | Emits `unknown_part_number` for an otherwise non-ignored subscription with a non-null part number and null policy. |
| `logPhysicalGuest` | Corrects a system referenced as a guest to `physical=false`; emits `physical_guest`. |
| `logMissingHost` | Treats a virtual system with neither a host nor explicit virtualization-group membership as physical; emits `guest_with_unknown_host`. |
| `logMissingCpuCount` | Sets an unknown CPU count to 16; emits `unknown_cpu_count`. |

Ignoring retains the subscription fact for reporting; it does not delete the subscription. Matchability rules require `ignored == false`. Removing an `InstalledProduct` removes a coverage requirement, not the product catalog entry itself.

OES exclusion is implemented through the five product IDs listed above, not a generic classification of every possible OES product. Correcting missing topology and substituting 16 CPUs can make the resulting allocation estimate conservative; the emitted warnings identify input that the caller should review.

### 3. Topology Augmentation

[InputAugmenting.drl](../src/main/resources/com/suse/matcher/deduction/rules/InputAugmenting.drl) creates facts needed by virtualization rules:

- `generateHostedProduct`: for a virtual guest's installed product and a `HostGuest` relationship, insert `HostedProduct(hostId, productId)`. This lets a host subscription be considered even when the product is only installed on a guest.
- `generatePenaltyGroupsFromVirtualizationGroups`: insert `PenaltyGroup(explicitGroupId, guestId)` for every explicit virtualization-group member.
- `generatePenaltyGroups`: for a guest with no explicit virtualization-group membership, insert a host-derived `PenaltyGroup`. Guests on the same host share that ID.

Explicit membership takes precedence over host-derived penalty grouping. These groups affect ONE_TWO capacity rounding during optimization; they do not independently confer subscription coverage.

#### What a Penalty Group Represents

[PenaltyGroup.java](../src/main/java/com/suse/matcher/deduction/facts/PenaltyGroup.java) represents **one guest's membership in an accounting group**, not a charge or a list of guests. `PenaltyGroup(id=7, guestId=A)` and `PenaltyGroup(id=7, guestId=B)` are two membership facts saying that guests A and B belong to the same group. The ID is shared; the guest ID distinguishes the facts.

For ONE_TWO subscriptions, the group is the boundary within which two selected guests may share one subscription unit. Explicit virtualization-group membership can place guests from different hosts in one accounting group. Without such membership, deduction uses the host ID to generate a shared group ID, so guests on different hosts are accounted for separately.

Deduction creates these memberships before any candidates are selected. During optimization, [Scores.drl](../src/main/resources/com/suse/matcher/optimization/rules/Scores.drl) uses them as follows:

1. `calculate12Penalties` identifies selected guests for each ONE_TWO subscription and group, creating `ConfirmedMatchInPenaltyGroup` facts. Unselected candidates do not count.
2. `insert12Penalties` counts those memberships separately for each `(subscriptionId, penaltyGroupId)`. An odd count creates a 50-cent `OneTwoPenalty`; an even count needs no extra charge.
3. `dontExceedSubscriptionCount` adds this rounding charge to ordinary selected cent-group charges and compares the total with the subscription's capacity.

In the simple case of one 50-cent candidate charge per guest, selecting A and B in group 7 consumes 100 cents with no rounding addition. Selecting A alone consumes 50 + 50 = 100 cents. Selecting A in group 7 and B in group 8 consumes 50 + 50 ordinary cents plus two 50-cent rounding charges, for 200 cents. Thus identical guest candidates can have different combined capacity requirements depending on their memberships.

Despite the name, a `PenaltyGroup` does not directly subtract score points. It supplies the grouping data used to derive extra capacity usage; exceeding capacity then affects the hard score. It does not force all guests in a group to be selected together, and guests using different subscriptions cannot pool their half units merely because they share a group ID. These rounding rules require the subscription's own policy to be ONE_TWO; inherited add-on subscriptions retain INHERITED_VIRTUALIZATION and do not receive these odd-group penalties. See the [optimization guide](optimization-phase.md) for the scoring details.

### 4. Stackable OEM Aggregation

[SubscriptionAggregation.drl](../src/main/resources/com/suse/matcher/deduction/rules/SubscriptionAggregation.drl) consolidates stackable OEM subscriptions, recognized by negative subscription IDs.

`aggregateEqualStackableSubscriptions` has salience 2. It seeks a leading non-ignored, stackable, negative-ID subscription that has no smaller eligible leading subscription, then a higher negative-ID subscription with quantity 1 and equal part number, name, policy, CPU entitlement, SCC username, and product set. It adds the quantities to the leader and takes the intersection of validity periods: later start date and earlier end date. The rule does not require initially identical dates.

`adjustPinnedMatches` has salience 1, redirecting pins to the aggregate before cleanup. Lower-priority rules remove absorbed subscriptions and their orphaned `SubscriptionProduct` facts. `adjustAggregatedSubscriptionName` prefixes the surviving name with `Aggregated:`. Messages include `merge_subscriptions` and `adjust_pinned_match`.

#### Why Salience Matters Here

An **activation** is one eligible execution of a rule for a particular combination of matching facts. Several activations may be waiting at once. Salience is an integer priority used to choose among eligible activations in the focused group: higher values take precedence, and an omitted salience has the default value 0. It is an execution priority, not a subscription quantity, penalty, or optimization score.

| Priority | Rule(s) | Why this comes before the next stage |
| --- | --- | --- |
| 2 | `aggregateEqualStackableSubscriptions` | Establish the surviving subscription and its accumulated quantity while compatible subscription facts and original names remain available. |
| 1 | `adjustPinnedMatches` | Redirect pins while the absorbed subscription still exists and records the surviving ID. |
| 0 | `removeAggregatedSubscriptions`, `removeRedundantSubscriptionProducts`, `adjustAggregatedSubscriptionName` | Remove obsolete facts and rename the survivor after the higher-priority merge and pin adjustments. |

After each firing, inserted or modified facts may create new activations or invalidate existing ones. Drools chooses again from the eligible activations; it does not execute every rule once in file order. While a salience-2 merge is eligible, it takes precedence over salience-1 pin adjustments. Once those merges are exhausted, pin adjustments take precedence over salience-0 cleanup. These priorities do not specify a complete ordering among rules at the same salience, nor do they override which agenda group has focus.

For example, suppose compatible OEM subscriptions -3 and -2 each have quantity 1, and system X is pinned to -2:

1. The salience-2 merge makes -3 the survivor with quantity 2 and records `aggregatedSubscriptionId=-3` on -2. Subscription -2 is still present.
2. The salience-1 pin rule uses that record to insert `PinnedMatch(X, -3)` and retract `PinnedMatch(X, -2)`.
3. Salience-0 cleanup removes -2 and its product associations; the survivor's name is prefixed with `Aggregated:`.

If the absorbed subscription were removed before the pin rule fired, that rule could no longer join the pin to the subscription carrying its replacement ID. Also, renaming the survivor too early could prevent further merges because the merge rule compares names. Salience preserves the data needed by the later operations and delays destructive cleanup and renaming.

This is the precise rule eligibility, not a promise to group all conceivable equivalent subscriptions: the leading-subscription search is part of its behavior.

Independent subscription IDs cannot be combined to cover the same system/product by confirming several overlapping groups. Aggregation provides a single larger capacity bucket before matching by merging eligible negative-ID subscriptions under the conditions above. Equal `sccUsername` is one aggregation condition, not a general partitioning or support-level compatibility constraint. The aggregation rule does not explicitly compare `supportLevel`, and there is no general support-level compatibility score or move filter.

### 5. Legacy Hard Bundle Conversion

[HardBundleConversion.drl](../src/main/resources/com/suse/matcher/deduction/rules/HardBundleConversion.drl) turns a legacy bundle represented by several subscriptions into one subscription:

1. `markHardBundledCandidates` identifies non-ignored, not-already-single-subscription bundles with equal part number, start date, end date, and quantity, and assigns provisional bundle IDs.
2. `mergeHardBundles` merges toward the lower subscription ID when the product sets are disjoint and the merging subscription has products. Product associations move to the leader, which is marked `singleSubscriptionHardBundle=true`.
3. `mergePinnedMatches` redirects pins from an absorbed subscription to the surviving bundle ID using insertion and retraction.
4. `removeRedundantSubscriptions` removes absorbed subscriptions, concatenates available names, and emits `hb_merge_subscriptions` messages.

Unlike OEM aggregation, bundle conversion does not sum the subscription quantities. Its purpose is to combine the covered product sets under one entitlement. The matching criteria are the fields listed above, not a general equality comparison of every subscription attribute.

Soft-bundle components supplied as separate subscriptions remain independently allocatable; there is no dedicated soft-bundle linking rule. A hard bundle shares capacity for eligible products on the same system, but does not require every bundled product to be installed or selected. Using only one eligible component can still consume the full charge. Different product classes are checked when sharing cent groups, not when initially identifying and merging legacy subscription records.

### 6. Matchability and Capacity Groups

[Matchability.drl](../src/main/resources/com/suse/matcher/deduction/rules/Matchability.drl) is the main bridge to optimization. For each eligible system/product/subscription combination, it describes a possible allocation and how much capacity that allocation would consume. It does not consume that capacity immediately or check whether all possible allocations fit together.

Think of each generated candidate as answering three separate questions:

1. **What could be covered?** A `PotentialMatch` associates a system, a product, and a subscription.
2. **What must be selected together?** Its `groupId` links rows belonging to one all-or-nothing option.
3. **What would be charged?** Its `centGroupId` references a `CentGroup` carrying a shared capacity amount.

Its central facts are:

| Fact/field | Meaning |
| --- | --- |
| `PotentialMatch(systemId, productId, subscriptionId, centGroupId, groupId)` | One candidate coverage row. |
| `groupId` | All rows with this ID must be selected together or not at all. |
| `CentGroup(id, cents)` | A shared subscription-capacity charge, in hundredths of a unit. These are not monetary cents. |
| `centGroupId` | Links a row to its capacity charge; several rows can share one charge. |
| `GroupInInheritedVirtualization(groupId)` | Marks a generated inherited group to prevent recursive inherited-group generation. |

#### Selection Groups Versus Capacity Groups

`groupId` is a selection boundary. Optimization creates one `Match` for that ID; selecting it selects every `PotentialMatch` row in that group. For example, ordinary unlimited-virtualization host coverage and its free guest rows share a selection group, so the solver cannot independently select those guest rows without selecting that option.

`centGroupId` is an accounting boundary, not a selection boundary. Multiple selected rows referencing the same charge for a subscription consume that charge once. For example, two hard-bundle products on one system can have different selection groups but share one 100-cent charge. Selecting either one consumes 100 cents; selecting both still consumes 100 cents. Conversely, an inherited base/add-on option has one selection group but separate charges against two subscriptions.

`PenaltyGroup`, described above, serves a third purpose: grouping selected ONE_TWO guests for rounding. Its ID is neither a candidate selection ID nor a cent-group reference. It does not tie those guests into an all-or-nothing option.

#### What a `groupId` Identifies

A `groupId` is the identifier of a **single solver decision**. Each `PotentialMatch` row carries the `groupId` assigned by a matchability rule. Later, [Matcher.java](../src/main/java/com/suse/matcher/Matcher.java) collects the distinct IDs and creates one optimization `Match` for each ID. The `Match.confirmed` planning variable is therefore attached to the group, not to each row: `true` selects every row with that ID, and `false` selects none. The numeric ID itself has no licensing meaning; its meaning comes from which rule generated it and which rows share it.

For example, if physical host H and guest G both run product P, and subscription S covers P with unlimited virtualization, deduction can create rows `(H, P, S)` and `(G, P, S)` with the same `groupId` derived from H and S. The guest row has zero additional usage, but it is still part of the same selection decision as the host row. The solver cannot select that derived guest coverage while rejecting its host/subscription group.

Inherited virtualization creates a different kind of shared decision. If system X has base product B and add-on A, a base candidate for `(X, B, S1)` and an inherited add-on candidate for `(X, A, S2)` can be emitted with one generated `groupId`. Confirming its `Match` selects both rows together, even though they refer to different products and subscriptions. Their capacity is still charged separately through their `centGroupId`/`CentGroup` facts. Thus `groupId` says **what is selected together**; it does not say **what shares a capacity charge**.

#### What Subscription Cents Mean

The implementation measures capacity in hundredths of a subscription unit, called **cents**:

- 100 cents means one subscription unit.
- 200 cents means two units.
- 50 cents means half a unit, used for an eligible ONE_TWO guest before rounding.
- 0 cents means no additional capacity usage, as for a guest covered through ordinary unlimited-virtualization host matching.

These amounts are not prices and do not count covered products. A shared 100-cent charge can cover several eligible rows; a single physical installation can require a 200-cent charge. A normal purchased quantity of 3 supplies 300 cents of capacity. Deduction records possible charges; optimization counts the distinct charges actually selected, adds applicable ONE_TWO rounding, and compares them with the available capacity.

#### Converting CPU Requirements into Whole Units

For the physical ONE_TWO and unlimited-virtualization rules that compute CPU-based consumption, let:

- $C$ be the system's supplied CPU count.
- $L$ be the CPU count covered by **one** unit of the applicable subscription.
- $N$ be the number of subscription units needed for that system.

The counts must use the same entitlement unit, such as sockets, and $L$ must be positive. CPU entitlement per unit is different from purchased quantity: a subscription covering two sockets per unit with quantity 3 offers three such units, not a six-socket value in its `cpus` field.

Choose the smallest whole number $N$ satisfying $NL\ge C$. This is division rounded **up**, written with the ceiling symbol:

$$
N=\left\lceil\frac{C}{L}\right\rceil,
\qquad \text{charge in cents}=100N.
$$

For example, a three-socket system and a two-socket-per-unit subscription need two units: one covers only two sockets, while two cover up to four. There is no 1.5-unit charge for this physical-system calculation.

Java integer division discards the fractional part for positive values, so `3 / 2` would incorrectly produce 1 for that requirement. The rule instead uses `(C + L - 1) / L`, which rounds up for positive counts within the integer range. With $C=3$ and $L=2$, it evaluates `(3 + 2 - 1) / 2`, or `4 / 2`, giving 2 units and therefore 200 cents. If $C$ is already an exact multiple of $L$, the expression leaves the required unit count unchanged.

| System count $C$ | CPUs per unit $L$ | Required units $N$ | Charge |
| --- | --- | --- | --- |
| 1 | 2 | 1 | 100 cents |
| 2 | 2 | 1 | 100 cents |
| 3 | 2 | 2 | 200 cents |
| 4 | 2 | 2 | 200 cents |
| 5 | 2 | 3 | 300 cents |

This formula determines the charge **after policy eligibility is satisfied**; it does not make every subscription stackable. For example, PHYSICAL_ONLY requires one unit's CPU entitlement to cover the whole system and then charges 100 cents; a non-stackable unlimited-virtualization entry must also satisfy its CPU limit before a candidate is generated. INSTANCE and virtual ONE_TWO candidates use their own fixed-charge rules instead of this physical-system formula.

#### Policy-by-Policy Summary

The following table combines eligibility, charging, and grouping. A "keyed by" description identifies which input values are passed to `idGenerator`: equal keys reuse an ID, so the rows share the specified selection or charge. Every subscription used by these rules must be non-ignored.

| Rule | Eligibility | Generated usage and grouping |
| --- | --- | --- |
| `matchPhysicalOnlySubscription` | Physical system; installed product offered by a non-ignored PHYSICAL_ONLY subscription; $L \ge C$. | 100 cents. Candidate/cent ID is keyed by system, product, and subscription. |
| `matchInstanceSubscription` | Any system; installed product offered by a non-ignored INSTANCE subscription; CPU entitlement absent or sufficient. | 100 cents with the same per-system/product/subscription key. No free guest inheritance. |
| `matchUnlimitedVirtualizationSubscription` | Physical system with known CPUs; installed or hosted product offered by a non-ignored UNLIMITED_VIRTUALIZATION subscription; enough CPUs or stackable. | $100\lceil C/L \rceil$ cents. Candidate group keyed by host and subscription, so eligible products on that host share a selection and charge. |
| `matchFreeVirtualizationSubscription` | An existing unlimited-virtualization match on a host, a host/guest relation, and that product installed on the guest. | Guest row with a zero-cent group, but the host/subscription selection group. Selecting that group selects host and derived guest coverage together. |
| `match12Subscription` | Installed product offered by a non-ignored ONE_TWO subscription; system has known CPUs and does not appear as a host in `HostGuest`. | Physical system: $100\lceil C/L \rceil$ cents. Virtual system: 50 cents. Candidate/cent ID keyed by system, product, and subscription. |
| `matchInheritedVirtualization` | Non-ignored INHERITED_VIRTUALIZATION add-on; add-on installed or hosted on a system; a base-product candidate on the same system; that base candidate is not already an inherited group. | New all-or-nothing group containing base and add-on rows. Separate base/add-on cent groups each copy the base row's usage. |

The ONE_TWO no-host condition applies even if the host's own installed product could otherwise match. The rule does not test `stackable` for physical ONE_TWO consumption. Unlimited virtualization can also derive host coverage from `HostedProduct`; such a host row need not correspond to an installed requirement and therefore need not earn a coverage reward itself.

Inherited virtualization creates an additional option rather than deleting the original base option. Its new group depends on the base candidate's policy and charge, not on a separately computed add-on CPU policy. `no-loop` and the marker fact prevent newly generated inherited groups from being recursively reused by that rule. Unlimited guest propagation can also use an eligible base row in such a composite group; the free guest rule explicitly derives its group key from host and subscription.

#### Bundle Charge Sharing

`mergeCentGroupsInBundles` finds candidate rows for the same system and single-subscription hard bundle, with different products, different non-null `productClass` values, and equal charges. It points the higher cent-group ID at the lower one. It changes `centGroupId`, not `groupId`: bundle rows can still be separate selectable groups while sharing one capacity charge.

`retractOrphanedCentGroups` removes charges that no candidate references. Sharing a charge is the reason that two covered bundle products need not consume two purchased units. Products in the same product class do not satisfy this merge rule.

## Worked Examples

### Unlimited Virtualization

Suppose physical host H has four CPUs, guest G runs product P, and subscription S covers P with UNLIMITED_VIRTUALIZATION and two CPUs per unit, stackable.

1. Input creates `HostGuest(H, G)` and `InstalledProduct(G, P)`.
2. Augmentation creates `HostedProduct(H, P)`.
3. Host matching creates a candidate H/P/S and a 200-cent charge: two purchased units.
4. Guest matching creates G/P/S with a zero-cent charge and the same selection group as the host.
5. Optimization can select both rows together if the total subscription capacity permits it. The guest's installed requirement contributes to coverage scoring; the hosted-only host row does not create an additional installed requirement.

### Inherited Add-on

Suppose system X has base product B and add-on A. A base subscription provides a 100-cent candidate for B; another subscription offers A under INHERITED_VIRTUALIZATION. The inherited rule creates a new group containing X/B/base-subscription and X/A/add-on-subscription, charged 100 cents to each subscription. Selecting this group covers both products; it conflicts with the standalone base candidate because both cover X/B.

### ONE_TWO Guests

Two eligible guests receive individual 50-cent candidates. Deduction also supplies their penalty-group membership. Optimization adds another 50 cents for each odd-sized selected guest population within a subscription/penalty-group pair. Thus two guests in one group consume 100 cents, while one guest in each of two groups consumes 200 cents. Details are in the [optimization guide](optimization-phase.md).

## Handoff, Output, and Troubleshooting

After deduction, [Matcher.java](../src/main/java/com/suse/matcher/Matcher.java):

1. Creates one initially uninitialized `Match` per distinct `PotentialMatch.groupId`.
2. Groups candidates by `(systemId, productId)` to identify incompatible selection groups.
3. Creates a sorted, distinct candidate cache for custom swap moves.
4. Constructs `Assignment` with those entities and all surviving deduced facts.

Deduction does not reserve capacity, honor pins as mandatory assignments, or choose a globally feasible subset. Those responsibilities belong to the solver and its move machinery.

After optimization, output conversion emits rows from selected groups. For each referenced cent group, it divides the charge by the number of selected rows sharing that group, using integer division. This reporting split must not be confused with how optimization counts a shared charge once. The output also contains the surviving, possibly aggregated or bundled subscriptions and diagnostic messages.

For an unexplained missing candidate, check policy enrichment, `ignored`, corrected physical/CPU values, installed versus hosted products, product associations, and the exact policy eligibility above. Deduction event logging is configured in [kmodule.xml](../src/main/resources/META-INF/kmodule.xml); the facade logs candidate counts and, at debug level, sorted candidate rows.

The [scenario test](../src/test/java/com/suse/matcher/MatcherScenariosTest.java) exercises both phases and compares JSON and CSV results with stored fixtures. Its scenario descriptions are useful concrete examples, but the source rules are authoritative for current conditions.

## Library References

- [Drools documentation](https://docs.drools.org/): facts, working memory, DRL patterns, agenda groups, property reactivity, and truth maintenance.
- [Apache KIE](https://kie.apache.org/): the project family and its rule-engine/planning libraries.
- [OptaPlanner reference manual](https://docs.optaplanner.org/latestFinal/optaplanner-docs/html_single/): background for the downstream planning model.

Library documentation may describe other releases or APIs. For this checkout, use the dependency versions in [pom.xml](../pom.xml) and the actual DRL/session configuration above when interpreting examples.
