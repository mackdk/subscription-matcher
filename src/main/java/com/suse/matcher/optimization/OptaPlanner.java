package com.suse.matcher.optimization;

import com.suse.matcher.optimization.facts.OneTwoPenalty;
import com.suse.matcher.util.CollectionUtils;
import com.suse.matcher.util.ExecutionTimer;

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
public class OptaPlanner implements OptimizationEngine {

    /** Logger instance. */
    private static final Logger LOGGER = LogManager.getLogger(OptaPlanner.class);

    private final SolverConfig configuration;

    private final SolverFactory<Assignment> solverFactory;

    /** The configured OptaPlanner solver reused across optimization calls. */
    private final Solver<Assignment> solver;

    /**
     * Instantiates an OptaPlanner optimizer.
     *
     * @param testing true if running as a unit test, false otherwise
     */
    public OptaPlanner(boolean testing) {
        configuration = createConfiguration(testing);
        solverFactory = SolverFactory.create(configuration);
        solver = solverFactory.buildSolver();
    }

    @Override
    public Assignment optimize(Assignment unsolved) {
        // short circuit the planning in case there's nothing to optimize
        if (unsolved.getMatches().isEmpty()) {
            return unsolved;
        }

        // solve problem
        Assignment solution = ExecutionTimer.measure("Optimization phase", () -> solver.solve(unsolved));
        LOGGER.info("{} matches confirmed", solution.getMatches().stream().filter(m -> m.isConfirmed()).count());

        if (LOGGER.isDebugEnabled()) {
            // Log confirmed matches
            solution.getMatches().stream()
                .filter(m -> m.isConfirmed())
                .forEach(m -> LOGGER.debug("{}", m));

            // Show the Penalty facts generated in Scores.drl using DroolsScoreDirector and re-calculating the score
            // of the best solution, because facts generated dynamically are not available outside of this object
            if (solverFactory instanceof DefaultSolverFactory<Assignment> defaultSolverFactory) {
                logOneTwoPenalties(defaultSolverFactory.getScoreDirectorFactory(), configuration.getEnvironmentMode(), solution);
            }
        }

        return solution;
    }

    private static void logOneTwoPenalties(InnerScoreDirectorFactory<Assignment, HardSoftScore> scoreDirectorFactory,
                                           EnvironmentMode environmentMode, Assignment result) {
        if (!(scoreDirectorFactory instanceof DrlScoreDirectorFactory<Assignment, HardSoftScore> drlScoreDirectorFactory)) {
            LOGGER.debug("Number of penalties for 1-2 subscriptions not available, score director factory is not a DRL one");
            return;
        }

        // Build a new score director and re-evaluate the score
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

}
