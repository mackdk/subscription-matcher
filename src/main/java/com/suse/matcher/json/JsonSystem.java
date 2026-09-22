package com.suse.matcher.json;

import java.util.Set;

/**
 * JSON representation of a system.
 * 
 * @param id
 * @param name profile name
 * @param cpus the number of CPUs
 * @param physical {@code true} if this system is made of metal
 * @param virtualHost {@code true} if this system is a virtual host
 * @param virtualSystemIds the id of the virtual machines hosted by this system
 * @param productIds the id of the products installed on this system
 */
public record JsonSystem(
    Long id,
    String name,
    Integer cpus,
    Boolean physical,
    Boolean virtualHost,
    Set<Long> virtualSystemIds,
    Set<Long> productIds
) {
}
