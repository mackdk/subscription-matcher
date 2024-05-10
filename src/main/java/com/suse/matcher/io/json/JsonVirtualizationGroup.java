package com.suse.matcher.io.json;

import java.util.Set;

/**
 * JSON representation of a group of virtual guests which belong to the same
 * cloud, VMWare vCenter, etc.
 * @param id the group id
 * @param name the name
 * @param type the type
 * @param virtualGuestIds the id of the the guest members of this group
 */
public record JsonVirtualizationGroup(Long id, String name, String type, Set<Long> virtualGuestIds) {
}
