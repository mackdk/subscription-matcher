/*
 * Copyright (c) 2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
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
