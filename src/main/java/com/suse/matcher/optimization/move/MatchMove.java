package com.suse.matcher.optimization.move;

import com.suse.matcher.optimization.Assignment;
import com.suse.matcher.optimization.Match;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * A custom-designed move for this specific problem.
 *
 * This will simply flip multiple "confirmed" bits in an equal number of {@link Match} planning entities.
 */
public class MatchMove extends AbstractMove<Assignment> {

    /** The matches. */
    private final List<Match> matches;

    /** The confirmed flags. */
    private final List<Boolean> confirmedFlags;

    /**
     * Instantiates a new match move.
     *
     * @param matchesIn the matches to flip
     * @param confirmedFlagsIn the new values of their confirmed flags. Must match in size()
     */
    public MatchMove(List<Match> matchesIn, List<Boolean> confirmedFlagsIn) {
        matches = matchesIn;
        confirmedFlags = confirmedFlagsIn;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Assignment> director) {
        for (int i = 0; i < matches.size(); i++) {
            Match m = matches.get(i);
            director.beforeVariableChanged(m, "confirmed");
            m.setConfirmed(confirmedFlags.get(i));
            director.afterVariableChanged(m, "confirmed");
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Assignment> director) {
        // MatchMoveIterator constructs doable MatchMoves only
        return true;
    }

    @Override
    public AbstractMove<Assignment> createUndoMove(ScoreDirector<Assignment> director) {
        List<Boolean> newConfirmedFlags = matches.stream()
                .map(m -> m.isConfirmed())
                .collect(Collectors.toList());

        return new MatchMove(matches, newConfirmedFlags);
    }

    @Override
    public Collection<Match> getPlanningEntities() {
        return matches;
    }

    @Override
    public Collection<Boolean> getPlanningValues() {
        return confirmedFlags;
    }

    @Override
    public int hashCode() {
        // MatchMoveIterator derives confirmedFlags from the Match objects.
        return Objects.hash(matches);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MatchMove other)) {
            return false;
        }

        // MatchMoveIterator derives confirmedFlags from the Match objects.
        return Objects.equals(matches, other.getPlanningEntities());
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", MatchMove.class.getSimpleName() + "[", "]");
        for (int i = 0; i < matches.size(); i++) {
            joiner.add(matches.get(i) + " -> " + confirmedFlags.get(i));
        }
        return joiner.toString();
    }
}
