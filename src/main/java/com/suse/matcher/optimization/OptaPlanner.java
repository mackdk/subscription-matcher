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
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirector;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
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
        Solver<Assignment> solver = initSolver(testing);

        // solve problem
        long start = System.currentTimeMillis();
        solver.solve(unsolved);
        LOGGER.info("Optimization phase took {}ms", System.currentTimeMillis() - start);
        result = solver.getBestSolution();
        LOGGER.info("{} matches confirmed", result.getMatches().stream().filter(m -> m.isConfirmed()).count());
        if (LOGGER.isDebugEnabled()) {
            // Log confirmed matches
            result.getMatches().stream()
                .filter(m -> m.isConfirmed())
                .forEach(m -> LOGGER.debug("{}", m));

            // Show the Penalty facts generated in Scores.drl using DroolsScoreDirector and re-calculating the score
            // of the best solution, because facts generated dynamically are not available outside of this object
            logOneTwoPenalties(solver, result);
        }
    }

    private static void logOneTwoPenalties(Solver<Assignment> solver, Assignment result) {
        // Make sure the runtime instances are of the correct types
        if (!(solver instanceof DefaultSolver) || !(solver.getScoreDirectorFactory() instanceof DroolsScoreDirectorFactory)) {
            return;
        }

        DefaultSolver<Assignment> defaultSolver = (DefaultSolver<Assignment>) solver;
        DroolsScoreDirectorFactory<Assignment> directorFactory = (DroolsScoreDirectorFactory<Assignment>) defaultSolver.getScoreDirectorFactory();
        EnvironmentMode environmentMode = defaultSolver.getEnvironmentMode();

        // Build a new score director and re-evaluate the score
        try (DroolsScoreDirector<Assignment> director = directorFactory.buildScoreDirector(true, environmentMode.isAsserted())) {
            director.setWorkingSolution(director.cloneSolution(result));
            director.calculateScore();

            Collection<OneTwoPenalty> penalties = CollectionUtils.typeStream(director.getKieSession().getObjects(), OneTwoPenalty.class)
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
    private Solver<Assignment> initSolver(boolean testing) {
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

    /**
     * Gets the result.
     *
     * @return the result
     */
    public Assignment getResult() {
        return result;
    }
}
