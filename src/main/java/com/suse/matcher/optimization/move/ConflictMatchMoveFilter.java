package com.suse.matcher.optimization.move;

import com.suse.matcher.optimization.Assignment;
import com.suse.matcher.optimization.Match;

import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMove;
import org.optaplanner.core.impl.score.director.ScoreDirector;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filters ChangeMoves by only accepting those that do not lead to conflicts.
 */
public class ConflictMatchMoveFilter implements SelectionFilter<Assignment, ChangeMove<Assignment>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(ScoreDirector<Assignment> director, ChangeMove<Assignment> move) {
        boolean confirmed = (Boolean) move.getPlanningValues().iterator().next();
        if (!confirmed) {
            // leaving a Match unconfirmed is always OK
            return true;
        }

        // we are confirming a Match
        Assignment solution = director.getWorkingSolution();
        Match match = (Match) move.getPlanningEntities().iterator().next();
        Set<Integer> conflictingIds = solution.getConflictingMatchIds(match.getId()).collect(Collectors.toSet());

        // accept this Move only if no conflicting Match has been confirmed already
        return solution.getMatches().stream().noneMatch(m -> m.isConfirmed() && conflictingIds.contains(m.getId()));
    }
}
