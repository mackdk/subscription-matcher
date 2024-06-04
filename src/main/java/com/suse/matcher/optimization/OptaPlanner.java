package com.suse.matcher.optimization;

import com.suse.matcher.optimization.facts.OneTwoPenalty;
import com.suse.matcher.util.CollectionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.optaplanner.constraint.drl.DrlScoreDirectorFactory;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolverFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * Facade on the OptaPlanner solver.
 *
 * Fills a Solution object.
 */
public class OptaPlanner {

    /** Logger instance. */
    private static final Logger LOGGER = LogManager.getLogger(OptaPlanner.class);

    /** The result. */
    private final Assignment result;

    /**
     * Instantiates an OptaPlanner instance with the specified unsolved problem.
     *
     * @param unsolved the unsolved problem
     * @param testing true if running as a unit test, false otherwise
     */
    public OptaPlanner(Assignment unsolved, boolean testing) {
        // short circuit the planning in case there's nothing to optimize
        if (unsolved.getMatches().isEmpty()) {
            result = unsolved;
            return;
        }

        // init solver
        SolverConfig configuration = createConfiguration(testing);
        SolverFactory<Assignment> solverFactory = SolverFactory.create(configuration);
        Solver<Assignment> solver = solverFactory.buildSolver();

        // solve problem
        long start = System.currentTimeMillis();
        result = solver.solve(unsolved);

        LOGGER.info("Optimization phase took {}ms", System.currentTimeMillis() - start);
        LOGGER.info("{} matches confirmed", result.getMatches().stream().filter(m -> m.isConfirmed()).count());

        if (LOGGER.isDebugEnabled()) {
            // Log confirmed matches
            result.getMatches().stream()
                .filter(m -> m.isConfirmed())
                .forEach(m -> LOGGER.debug("{}", m));

            // Show the Penalty facts generated in Scores.drl using DroolsScoreDirector and re-calculating the score
            // of the best solution, because facts generated dynamically are not available outside of this object
            if (solverFactory instanceof DefaultSolverFactory<?>) {
                var defaultSolverFactory = (DefaultSolverFactory<Assignment>) solverFactory;
                logOneTwoPenalties(defaultSolverFactory.getScoreDirectorFactory(), configuration.getEnvironmentMode(), result);
            }
        }
    }

    private static void logOneTwoPenalties(InnerScoreDirectorFactory<Assignment, HardSoftScore> scoreDirectorFactory,
                                           EnvironmentMode environmentMode, Assignment result) {
        if (!(scoreDirectorFactory instanceof DrlScoreDirectorFactory<?, ?>)) {
            LOGGER.debug("Number of penalties for 1-2 subscriptions not available, score director factory is not a DRL one");
            return;
        }

        // Build a new score director and re-evaluate the score
        var drlScoreDirectorFactory = (DrlScoreDirectorFactory<Assignment, HardSoftScore>) scoreDirectorFactory;
        try (var director = drlScoreDirectorFactory.buildScoreDirector(true, environmentMode.isAsserted())) {
            director.setWorkingSolution(director.cloneSolution(result));
            director.calculateScore();

            var penalties = CollectionUtils.typeStream(director.getKieSession().getObjects(), OneTwoPenalty.class)
                .collect(Collectors.toList());

            LOGGER.debug("The best solution has {} penalties for 1-2 subscriptions.", penalties.size());
            penalties.forEach(penalty -> LOGGER.debug("{}", penalty));
        }
        catch (Exception ex) {
            LOGGER.debug("Number of penalties for 1-2 subscriptions not available: {}", ex.getMessage());
        }
    }

    /**
     * Configures and returns an OptaPlanner solver.
     *
     * This method replaces the XML configuration file cited in OptaPlanner's documentation.
     *
     * @return the solver
     * @param testing true if running as a unit test, false otherwise
     */
    private SolverConfig createConfiguration(boolean testing) {
        try (InputStream stream = OptaPlanner.class.getResourceAsStream("solver-config.xml")) {
            if (stream == null) {
                throw new IllegalStateException("Unable to locate planner configuration");
            }

            SolverConfig config = SolverConfig.createFromXmlInputStream(stream);

            // Tweak parameters in unit tests, which deal with fewer data and need to run faster.
            if (testing) {
                // Activate OptaPlanner full assertions to catch more issues
                config.setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
                // Reduce the number of steps we accept with no improvement during the local search phase
                config.getPhaseConfigList().stream()
                    .filter(phaseConfig -> phaseConfig instanceof LocalSearchPhaseConfig)
                    .map(PhaseConfig::getTerminationConfig)
                    .forEach(terminationConfig -> terminationConfig.setUnimprovedStepCountLimit(12));
            }

            return config;
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to parse planner configuration", ex);
        }
    }

    /**
     * Gets the result.
     *
     * @return the result
     */
    public Assignment getResult() {
        return result;
    }
}
