package com.suse.matcher.optimization;

/**
 * Optimizes a matching problem into an assignment that best satisfies the configured constraints.
 */
public interface OptimizationEngine {

    /**
     * Solves the provided assignment problem and returns the optimized result.
     *
     * @param unsolved the unsolved assignment to optimize
     * @return the optimized assignment
     */
    Assignment optimize(Assignment unsolved);
}
