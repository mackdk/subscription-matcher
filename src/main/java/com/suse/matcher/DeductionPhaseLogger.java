package com.suse.matcher;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.rule.RuleContext;

import java.util.Optional;

public class DeductionPhaseLogger implements AgendaEventListener, RuleRuntimeEventListener, ProcessEventListener {
    private static final Logger LOGGER = LogManager.getLogger(DeductionPhaseLogger.class);

    private static final Marker AGENDA = MarkerManager.getMarker("AgendaEvent");

    private static final Marker RULE_RUNTIME = MarkerManager.getMarker("RuleRuntimeEvent");

    private static final Marker PROCESS = MarkerManager.getMarker("ProcessEvent");

    private static final Marker RULE = MarkerManager.getMarker("RuleLog");

    public static void trace(RuleContext context, String message, Object... parameters) {
        ruleLog(Level.TRACE, context, message, parameters);
    }

    public static void debug(RuleContext context, String message, Object... parameters) {
        ruleLog(Level.DEBUG, context, message, parameters);
    }

    public static void info(RuleContext context, String message, Object... parameters) {
        ruleLog(Level.INFO, context, message, parameters);
    }

    public static void warn(RuleContext context, String message, Object... parameters) {
        ruleLog(Level.WARN, context, message, parameters);
    }

    public static void error(RuleContext context, String message, Object... parameters) {
        ruleLog(Level.ERROR, context, message, parameters);
    }

    private static void ruleLog(Level level, RuleContext context, String message, Object... parameters) {
        Marker ruleMarker = MarkerManager.getMarker(context.getRule().getName());
        ruleMarker.setParents(RULE);

        LOGGER.log(level, ruleMarker, message, parameters);
    }

    @Override
    public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
        LOGGER.debug(AGENDA, "Group '{}' popped", event.getAgendaGroup().getName());
    }

    @Override
    public void agendaGroupPushed(AgendaGroupPushedEvent event) {
        LOGGER.debug(AGENDA, "Group '{}' pushed", event.getAgendaGroup().getName());
    }

    @Override
    public void beforeMatchFired(BeforeMatchFiredEvent event) {
        LOGGER.debug(AGENDA, "Match for rule '{}'", event.getMatch().getRule().getName());
    }

    @Override
    public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
        LOGGER.debug(AGENDA, "Rule flow group '{}' activated", event.getRuleFlowGroup().getName());
    }

    @Override
    public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
        LOGGER.debug(AGENDA, "Rule flow group '{}' deactivated", event.getRuleFlowGroup().getName());
    }

    @Override
    public void objectInserted(ObjectInsertedEvent event) {
        LOGGER.debug(RULE_RUNTIME,  "A {} was inserted {}. Value is {}",
            () -> getFactName(event.getObject()), () -> getEventTrigger(event.getRule()), () -> event.getObject()
        );
    }

    @Override
    public void objectUpdated(ObjectUpdatedEvent event) {
        LOGGER.debug(RULE_RUNTIME, "A {} was updated {}. Value now is {}",
            () -> getFactName(event.getOldObject()), () -> getEventTrigger(event.getRule()), () -> event.getObject()
        );
    }

    @Override
    public void objectDeleted(ObjectDeletedEvent event) {
        LOGGER.debug(RULE_RUNTIME, "A {} was deleted {}. Value was {}",
            () -> getFactName(event.getOldObject()), () -> getEventTrigger(event.getRule()), () -> event.getOldObject()
        );
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        LOGGER.debug(PROCESS, "Process completed {}", event);
    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        LOGGER.debug(PROCESS, "Node triggered {}", event);
    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        LOGGER.debug(PROCESS, "Node Left {}", event);
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        LOGGER.debug(PROCESS, "Variable changed {}", event);
    }

    private static String getFactName(Object object) {
        return object.getClass().getSimpleName();
    }

    private static String getEventTrigger(Rule rule) {
        return Optional.ofNullable(rule)
            .map(r -> "by rule '" + r.getName() + "'")
            .orElse("manually");
    }

    //#region The rest of the methods are just no-op to reduce noise
    @Override
    public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
        // Not useful to log
    }

    @Override
    public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
        // Not useful to log
    }

    @Override
    public void matchCreated(MatchCreatedEvent event) {
        // Not useful to log
    }

    @Override
    public void matchCancelled(MatchCancelledEvent event) {
        // Not useful to log
    }

    @Override
    public void afterMatchFired(AfterMatchFiredEvent event) {
        // Not useful to log
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        // Not useful to log
    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        // Not useful to log
    }

    @Override
    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        // Not useful to log
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        // Not useful to log
    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        // Not useful to log
    }

    @Override
    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        // Not useful to log
    }
    //#endregion The rest of the methods are just no-op to reduce noise
}
