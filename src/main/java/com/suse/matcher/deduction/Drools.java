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
import java.util.Collection;

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

    /** Deduction resulting fact objects. */
    private final Collection<Object> result;

    /**
     * Instantiates a Drools instance with the specified base facts.
     * @param baseFacts fact objects
     * @param idGenerator the fact id generator
     */
    public Drools(Collection<Object> baseFacts, FactIdGenerator idGenerator) {
        // setup engine
        KieServices services = KieServices.get();
        KieContainer container = services.getKieClasspathContainer();

        // start a new session
        KieSession session = container.newKieSession();
        session.setGlobal("idGenerator", idGenerator);

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
}
