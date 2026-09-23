package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a hardware or software entity on which you can install
 * products and that can be assigned {@link Subscription}s.
 */
@PropertyReactive
public class System {
    /** The id. */
    private final long id;

    /** The friendly name. */
    private final String name;

    /** The populated CPU socket count. */
    private Integer cpus;

    /** <code>true</code> if this is a machine made of metal. */
    private boolean physical;

    /**
     * Instantiates a new system.
     *
     * @param idIn the id
     * @param nameIn the friendly name
     * @param cpusIn the cpus
     * @param physicalIn true if this system is made of metal
     */
    public System(long idIn, String nameIn, Integer cpusIn, boolean physicalIn) {
        id = idIn;
        name = nameIn;
        cpus = cpusIn;
        physical = physicalIn;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the cpus.
     *
     * @return the cpus
     */
    public Integer getCpus() {
        return cpus;
    }

    /**
     * Sets the cpus.
     *
     * @param cpusIn  the cpus
     */
    public void setCpus(Integer cpusIn) {
        this.cpus = cpusIn;
    }

    /**
     * Checks if this system is physical.
     *
     * @return true, if it is physical
     */
    public boolean isPhysical() {
        return physical;
    }

    /**
     * Sets if this system is physical.
     *
     * @param physicalIn  true, if it is physical
     */
    public void setPhysical(boolean physicalIn) {
        this.physical = physicalIn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof System other)) {
            return false;
        }
        return Objects.equals(id, other.getId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", System.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .toString();
    }
}
