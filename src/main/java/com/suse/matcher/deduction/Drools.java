package com.suse.matcher.deduction;

import com.suse.matcher.deduction.facts.PotentialMatch;
import com.suse.matcher.util.CollectionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.Agenda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade on the Drools rule engine.
 *
 * Deduces facts based on some base facts and rules defined ksession-rules.xml.
 */
public class Drools {
    /** Logger instance. */
    private static final Logger LOGGER = LogManager.getLogger(Drools.class);

    /** Rule groups corresponding to filenames and agenda group names. */
    private static final String[] RULE_GROUPS = {
        "PartNumbers",
        "InputValidation",
        "InputAugmenting",
        "SubscriptionAggregation",
        "HardBundleConversion",
        "Matchability",
    };

    /** Map to fact ids, see generateId(). */
    private static final Map<List<Object>, Integer> ID_MAP = new HashMap<>();

    /** Deduction resulting fact objects. */
    private final Collection<Object> result;

    /**
     * Instantiates a Drools instance with the specified base facts.
     * @param baseFacts fact objects
     */
    public Drools(Collection<Object> baseFacts) {
        // setup engine
        KieServices services = KieServices.get();
        KieContainer container = services.getKieClasspathContainer();

        // start a new session
        KieSession session = container.newKieSession();

        try {
            // set rule ordering
            Agenda agenda = session.getAgenda();
            for (int i = RULE_GROUPS.length - 1; i >= 0; i--) {
                agenda.getAgendaGroup(RULE_GROUPS[i]).setFocus();
            }

            // insert base facts
            for (Object fact : baseFacts) {
                session.insert(fact);
            }

            // start deduction engine
            long start = System.currentTimeMillis();
            session.fireAllRules();
            LOGGER.info("Deduction phase took {}ms", System.currentTimeMillis() - start);

            // collect results
            result = new ArrayList<>(session.getObjects());

            LOGGER.info("Found {} potential matches", CollectionUtils.typeStream(result, PotentialMatch.class).count());
            if (LOGGER.isDebugEnabled()) {
                CollectionUtils.typeStream(result, PotentialMatch.class)
                    .sorted()
                    .forEach(pm -> LOGGER.debug("{}", pm));
            }

        }
        finally {
            // cleanup
            session.dispose();
        }
    }

    /**
     * Returns all facts deduced by Drools.
     * @return the deduced facts
     */
    public Collection<Object> getResult() {
        return result;
    }

    /**
     * reset the idMap
     */
    public static void resetIdMap() {
        ID_MAP.clear();
    }

    /**
     * Returns a sequential id which is unique to the specified data.
     *
     * Equal input data always results in the same id.
     *
     * @param objects objects to generate this id from
     * @return a new id
     */
    public static int generateId(Object... objects) {
        return ID_MAP.computeIfAbsent(Arrays.asList(objects), k -> ID_MAP.size());
    }
}
