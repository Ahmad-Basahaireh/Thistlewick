package com.thistlewick.domain.event;

import java.time.LocalDateTime;

/**
 * Marker interface for all domain events in Thistlewick.
 *
 * <p>Sealed to restrict implementations to the event package. This
 * gives us compile-time exhaustiveness and prevents accidental
 * implementations elsewhere.</p>
 */
//Sealed = "مُختوم". يعني يُقيّد من يمكنه تنفيذ الـ interface
    //كلمة permits: تحدد من يُسمح له التنفيذ.
public sealed interface DomainEvent
        permits TaskCreatedEvent, TaskCompletedEvent,
        TaskOverdueEvent, TaskRescheduledEvent {

    LocalDateTime occurredAt();
}