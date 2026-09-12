package com.thistlewick.patterns.observer;

import com.thistlewick.domain.event.DomainEvent;

import java.util.logging.Logger;

/**
 * Observer that logs every domain event. Registered for
 * {@link DomainEvent} itself so it catches all subclasses.
 */
public class LoggingObserver implements EventObserver<DomainEvent> {

    private static final Logger LOG = Logger.getLogger(LoggingObserver.class.getName());

    @Override
    public void onEvent(DomainEvent event) {
        LOG.info(() -> String.format("[EVENT] %s @ %s",
                event.getClass().getSimpleName(), event.occurredAt()));
    }
}