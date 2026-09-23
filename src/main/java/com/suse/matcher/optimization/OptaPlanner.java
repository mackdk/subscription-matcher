package com.suse.matcher.optimization;

import com.suse.matcher.optimization.facts.OneTwoPenalty;
import com.suse.matcher.util.CollectionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * Facade on the OptaPlanner solver.
 *
 * Fills a Solution object.
 */
public class OptaPlanner implements OptimizationEngine {

    /** Logger instance. */
    private static final Logger LOGGER = LogManager.getLogger(OptaPlanner.class);

    /** The configured OptaPlanner solver reused across optimization calls. */
    private final Solver<Assignment> solver;

    /**
     * Instantiates an OptaPlanner optimizer.
     *
     * @param testing true if running as a unit test, false otherwise
     */
    public OptaPlanner(boolean testing) {
        this.solver = createSolver(testing);
    }

    @Override
    public Assignment optimize(Assignment unsolved) {
        // short circuit the planning in case there's nothing to optimize
        if (unsolved.getMatches().isEmpty()) {
            return unsolved;
        }

        // solve problem
        long start = System.currentTimeMillis();
        solver.solve(unsolved);

        LOGGER.info("Optimization phase took {}ms", System.currentTimeMillis() - start);
        Assignment solution = solver.getBestSolution();
        LOGGER.info("{} matches confirmed", solution.getMatches().stream().filter(m -> m.isConfirmed()).count());

        if (LOGGER.isDebugEnabled()) {
            // Log confirmed matches
            solution.getMatches().stream()
                .filter(m -> m.isConfirmed())
                .forEach(m -> LOGGER.debug("{}", m));

            // Show the Penalty facts generated in Scores.drl using DroolsScoreDirector and re-calculating the score
            // of the best solution, because facts generated dynamically are not available outside of this object
            logOneTwoPenalties(solver, solution);
        }

        return solution;
    }

    private static void logOneTwoPenalties(Solver<Assignment> solver, Assignment result) {
        // Make sure the runtime instances are of the correct types
        if (!(solver instanceof DefaultSolver<Assignment> defaultSolver) ||
                !(solver.getScoreDirectorFactory() instanceof DroolsScoreDirectorFactory<Assignment> directorFactory)) {
            return;
        }

        var environmentMode = defaultSolver.getEnvironmentMode();

        // Build a new score director and re-evaluate the score
        try (var director = directorFactory.buildScoreDirector(true, environmentMode.isAsserted())) {
            director.setWorkingSolution(director.cloneSolution(result));
            director.calculateScore();

            var penalties = CollectionUtils.typeStream(director.getKieSession().getObjects(), OneTwoPenalty.class)
                .toList();

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
    private Solver<Assignment> createSolver(boolean testing) {
        try (InputStream stream = OptaPlanner.class.getResourceAsStream("solver-config.xml")) {
            if (stream == null) {
                throw new IllegalStateException("Unable to locate planner configuration");
            }

            SolverFactory<Assignment> factory = SolverFactory.createFromXmlInputStream(stream);

            // Tweak parameters in unit tests, which deal with fewer data and need to run faster.
            if (testing) {
                SolverConfig config = factory.getSolverConfig();
                // Activate OptaPlanner full assertions to catch more issues
                config.setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
                // Reduce the number of steps we accept with no improvement during the local search phase
                config.getPhaseConfigList().stream()
                    .filter(phaseConfig -> phaseConfig instanceof LocalSearchPhaseConfig)
                    .map(PhaseConfig::getTerminationConfig)
                    .forEach(terminationConfig -> terminationConfig.setUnimprovedStepCountLimit(12));
            }

            return factory.buildSolver();
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to parse planner configuration", ex);
        }
    }

}
