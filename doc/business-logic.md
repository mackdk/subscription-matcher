# Subscription Matching Business Logic

## Purpose

`subscription-matcher` estimates how supplied subscription entitlements can be allocated to products installed on supplied systems. Its output identifies selected system/product/subscription associations, total and matched subscription quantities (from which a nominal remainder can be derived), uncovered installed products, and input or allocation messages.

The result depends on the inventory, topology, product catalog, subscription attributes, and part-number metadata supplied to the program. It is an allocation estimate, not a legal audit. A missing host relationship, an assumed CPU count, or an unknown part number can affect the result.

This guide introduces the domain model and decision flow. The [deduction guide](deduction-phase.md) explains how Drools creates candidate matches. The [optimization guide](optimization-phase.md) explains detailed scoring, penalties, and search moves.

## The Domain Model

| Item | Business meaning |
| --- | --- |
| System | A machine or virtual system with an ID, a physical/virtual flag, CPU count, and installed products. A physical system can also host guests. |
| Product | A product that may be installed and entitled. Product metadata includes whether it is free, whether it is a base product, and its product class. |
| Subscription | An entitlement with a part number, validity dates, quantity, product associations, and policy metadata. The catalog rules derive policy, CPU allowance, support level, and stackability from the part number. |
| Installed product | A product requirement on one system. Free and specifically blacklisted installed products are removed from matching requirements during input validation. |
| Subscription product | An association saying that a subscription can cover a particular product. It does not by itself mean the subscription is assigned. |
| Host/guest relationship | Topology stating that a guest runs on a host. It enables hosted-product and free-guest candidate generation. |
| Virtualization group | An upstream-supplied group of guests that share ONE_TWO rounding. If there is no explicit group membership, guests are grouped by host for this accounting. |
| Pin | A preference for a system/subscription pair. It earns a score reward when selected, but does not force an assignment or reserve a quantity. |

A subscription's **quantity** is how many units are available. Its **CPU allowance** is how many CPUs one unit covers where the policy uses CPU-based counting. These are different values: quantity three of a two-CPU subscription represents three units, each covering two CPUs.

### Product Bundles

A subscription can be associated with one product or with several products. A product association says that the subscription may cover that product; it does not, by itself, define whether related products must be assigned together or whether they share one unit of capacity. In this implementation, those behaviors depend on whether the products are presented as independent soft-bundle entitlements or as a hard bundle.

- **Soft bundle:** its component entitlements are represented as separate subscriptions. They remain independent choices and can be allocated to different systems. `subscription-matcher` does not add a rule that ties those subscription records together.
- **Hard bundle:** one subscription represents products from different non-null product classes. If that subscription can match two such products on the same system with equal usage charges, deduction can make their candidate rows share a `centGroupId`. Selecting either row consumes the full shared charge; selecting both still consumes it once. The rows may have different `groupId`s, so sharing capacity does not require both products to be installed or selected. Matching only one component can leave part of the bundle unused while still consuming one unit.

Some inputs represent a legacy hard bundle as several exploded subscription records. Before candidate generation, `subscription-matcher` can merge eligible records when their part number, dates, and quantity agree and their product lists are disjoint. It retains the lowest-ID subscription, combines the product associations, redirects pins to the retained ID, and removes the redundant records; it does not add their quantities together. This normalization is distinct from stackable OEM aggregation, which combines eligible stackable OEM records with the same product set to increase quantity.

Input preparation is an important part of the model. `subscription-matcher` uses supplied product and topology data; it does not discover hypervisors, infer OEM quantities from SCC order items, or decide maintenance-calendar eligibility for LTSS. See the [deduction guide's input contract](deduction-phase.md#upstream-contract).

## From Eligibility to Assignment

Matching has two stages:

1. **Generate candidates.** For each eligible system/product/subscription combination, including host-side candidates derived from guest installations, deduction checks policy-specific eligibility. A successful check creates a `PotentialMatch`: a possible system/product/subscription row with selection and capacity-group identifiers. This stage lists possibilities; it does not reserve subscription quantity or choose among alternatives.
2. **Select a feasible combination.** Optimization gives each distinct selection group one yes/no decision. It selects groups that avoid duplicate coverage and stay within subscription capacity, then prefers allocations with higher soft scores. A candidate can be individually eligible but not selected because it conflicts with another choice or there is not enough remaining capacity.

Three identifiers/groupings answer different questions:

- **Candidate row (`PotentialMatch`):** What system, product, and subscription could be associated?
- **Selection group (`groupId`):** Which candidate rows are confirmed together? One planning `Match` controls all rows with the same `groupId`.
- **Capacity group (`centGroupId` / `CentGroup`):** Which rows share one subscription-usage charge? The same charge is counted once even when several selected rows refer to it.

ONE_TWO has a third grouping concept, `PenaltyGroup`: it groups guest memberships for rounding half-unit usage. It is neither a selection group nor a capacity charge.

## Subscription Policies

Policy is derived from the subscription's part number. The table describes the current candidate-generation behavior; it is not a complete licensing contract. `C` is the system CPU count and `L` is the subscription CPU allowance per unit, expressed in the same counting units. Where the rules compute whole-unit CPU usage, they use $\lceil C/L \rceil$.

| Policy | Candidate eligibility and selected behavior | Initial capacity usage |
| --- | --- | --- |
| `PHYSICAL_ONLY` | The system must be physical, the product must be installed and associated with the subscription, and one unit's CPU allowance must cover the system. It does not grant free coverage to guests. | 100 cents for the candidate. The rule does not stack multiple units to cover a system that exceeds one unit's CPU allowance. |
| `UNLIMITED_VIRTUALIZATION` | The host candidate is for a physical system with known CPUs and a product installed on or hosted by that system. The catalog CPU allowance must cover the host, unless the subscription is stackable. Eligible guest installations can receive derived free rows. Host and derived guest rows share the host/subscription selection group. | Host usage is $100\lceil C/L \rceil$ cents. A derived guest row adds zero cents. |
| `ONE_TWO` | The system must have known CPUs, the product must be installed and associated with the subscription, and the system must not itself be listed as a host in `HostGuest`. A physical system is charged by CPU allowance; a virtual guest starts at half a unit. Selected guest usage is rounded within each subscription/penalty-group pair. | Physical system: $100\lceil C/L \rceil$ cents. Guest: 50 cents, plus 50 cents for each odd selected-guest count in that subscription's penalty group. |
| `INSTANCE` | An installed product can match on a physical or virtual system. If the catalog sets a CPU allowance, it must cover the system; otherwise there is no CPU limit check in this rule. It does not grant virtualization inheritance. | 100 cents per candidate charge. |
| `INHERITED_VIRTUALIZATION` | An installed or hosted add-on product can be paired with a base-product candidate on the same system. The base and add-on candidate rows are selected as one group, against their respective subscriptions. | The rule copies the base candidate's usage into separate charges for the base and add-on subscriptions. This does not implement every inherited-policy behavior: ONE_TWO odd-group rounding only applies when the charged subscription itself has policy `ONE_TWO`. |

The ONE_TWO host test is based on the `HostGuest` relationship, not the separate `virtualHost` input flag. A physical system with no listed guests can therefore be considered by the ONE_TWO candidate rule even if that flag says it is a host.

### Examples

**Unlimited virtualization:** Physical host H has four CPUs; guest G has product P installed; subscription S covers P with two CPUs per unit. Deduction creates an H/P/S host row charged 200 cents (two units) and a G/P/S derived row charged zero cents. Both rows share a selection group. Selecting that group selects both rows; it does not make the host row and guest row separate choices.

**ONE_TWO guests:** Two selected guests with 50-cent usage in one penalty group consume 100 cents in total. If each guest is in a different group, each group's odd count adds 50 cents, for 200 cents total. Guests in different groups cannot pool their half units for that subscription.

**Inherited add-on:** System X has base product B and add-on A. A candidate for B under subscription S1 and an inherited candidate for A under S2 can be placed in one selection group. Confirming the group selects both rows, but capacity is charged separately: the base charge to S1 and add-on charge to S2.

## Capacity Accounting: Cents

The implementation represents subscription usage in hundredths of one subscription unit. Its field is named `cents`, but these are **not monetary cents**:

- 100 cents is one subscription unit.
- 50 cents is half a unit before ONE_TWO guest rounding.
- 0 cents is no additional usage, such as a guest covered by an unlimited-virtualization host selection.

For a normal subscription quantity $q$, the modeled capacity is $100q$ cents. The optimizer adds distinct selected capacity charges for that subscription, then adds any applicable ONE_TWO rounding, and checks the total against this capacity. Some catalog entries set quantity to `Integer.MAX_VALUE` as a special effectively-unlimited sentinel. The scoring rule handles that value specially instead of multiplying it by 100.

Capacity groups are how duplicate rows avoid multiplying a charge. For example, if two eligible hard-bundle product rows on one system share the same cent group, selecting both consumes that charge once, not twice. The bundle merge rule only shares charges for its eligible single-subscription bundles, on the same system, across different non-null product classes, when the charges are equal. Sharing a capacity charge does not necessarily mean the rows have the same selection group.

## How the Best Allocation Is Chosen

Subscription usage is the hard part of the score: a capacity overrun gives a negative hard score, and no soft reward can compensate for it. Among equally feasible allocations, the solver maximizes soft rewards:

- 100 points for each covered installed-product match.
- 10 points for each selected match that joins a pin.
- 1 point for each qualifying bundled-match rule activation.

Pins are preferences, not mandatory assignments. Likewise, uncovered installed products are not a hard violation; they simply do not receive the coverage reward. Overlapping groups that cover the same system/product are prevented from being selected together by conflict-aware move generation, rather than by a dedicated score penalty.

The solver uses a construction heuristic followed by local search over its configured flip and swap moves. It returns the best-scoring solution found before termination, but this is a heuristic search and is not a guarantee of a globally optimal allocation. The assignment is therefore an estimate based on the supplied inputs and current part-number catalog.

## Further Reading

- [Deduction phase](deduction-phase.md): fact creation, input corrections, policy rules, topology augmentation, and candidate generation.
- [Optimization phase](optimization-phase.md): hard/soft scoring, logical accounting facts, termination, and move classes.
