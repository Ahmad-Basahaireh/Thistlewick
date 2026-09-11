package com.thistlewick.patterns.observer;

import com.thistlewick.domain.event.DomainEvent;

/**
 * Observer in the Pub-Sub pattern.
 *
 * <p>Observers must not throw: {@link EventBus} catches and logs
 * exceptions so one failing observer cannot block others.</p>
 *
 * @param <E> the specific event type this observer handles
 */
@FunctionalInterface
public interface EventObserver<E extends DomainEvent> {

    void onEvent(E event);
}