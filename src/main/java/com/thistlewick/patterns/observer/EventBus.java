package com.thistlewick.patterns.observer;

import com.thistlewick.domain.event.DomainEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EventBus — the broker of the Pub-Sub pattern.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li><b>Type-based dispatch:</b> observers register for a specific
 *       event class; publishing notifies only matching observers
 *       (including subtype observers).</li>
 *   <li><b>Thread-safe:</b> {@link ConcurrentHashMap} for the registry,
 *       {@link CopyOnWriteArrayList} for the observer lists — reads
 *       (publish) vastly outnumber writes (register).</li>
 *   <li><b>Failure isolation:</b> an observer's exception is logged
 *       and swallowed so the remaining observers still run.</li>
 * </ul>
 */
public final class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    private final Map<Class<? extends DomainEvent>,
            List<EventObserver<? extends DomainEvent>>> observers
            = new ConcurrentHashMap<>();

    /**
     * Registers an observer for a specific event type.
     */
    public <E extends DomainEvent> void register(
            Class<E> eventType, EventObserver<E> observer) {
        Objects.requireNonNull(eventType, "eventType required");
        Objects.requireNonNull(observer, "observer required");

        observers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(observer);
    }

    /**
     * Publishes an event to all registered matching observers.
     */
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event required");

        for (Map.Entry<Class<? extends DomainEvent>,
                List<EventObserver<? extends DomainEvent>>> entry
                : observers.entrySet()) {

            if (entry.getKey().isInstance(event)) {
                for (EventObserver<? extends DomainEvent> observer : entry.getValue()) {
                    dispatch(observer, event);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(EventObserver<? extends DomainEvent> observer,
                          DomainEvent event) {
        try {
            ((EventObserver<DomainEvent>) observer).onEvent(event);
        } catch (RuntimeException ex) {
            LOG.log(Level.SEVERE,
                    "Observer failed for event " + event.getClass().getSimpleName(), ex);
        }
    }
}