package com.suse.matcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects which numbered matcher scenarios should be executed by {@link MatcherScenariosTest}.
 * <p>
 * The selected set is computed as the inclusive range from {@code first} to
 * {@code last}, excluding any scenario numbers listed in {@code skip}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScenarioSelector {

    /**
     * The first scenario number to include (inclusive).
     *
     * @return the first scenario number
     */
    int first() default 1;

    /**
     * The last scenario number to include (inclusive).
     *
     * @return the last scenario number
     */
    int last() default Integer.MAX_VALUE;

    /**
     * Scenario numbers to exclude from the selected range.
     *
     * @return scenario numbers to skip
     */
    int[] skip() default {};

}
