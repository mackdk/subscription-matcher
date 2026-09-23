package com.suse.matcher.deduction;

import java.util.Collection;

/**
 * Deduces additional facts from matcher input facts.
 */
public interface DeductionEngine {

    /**
     * Deduces additional facts from the specified base facts.
     *
     * @param baseFacts fact objects
     * @return the base and deduced facts
     */
    Collection<Object> deduce(Collection<Object> baseFacts);
}
