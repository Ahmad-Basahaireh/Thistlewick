package com.thistlewick.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository abstraction for CRUD operations.
 *
 * <h2>Generics rationale</h2>
 * <ul>
 *   <li>{@code T} — the domain entity type (e.g. {@code User}).</li>
 *   <li>{@code ID} — the identity type (e.g. {@code Long}).</li>
 * </ul>
 *
 * <p>This interface is intentionally persistence-agnostic: JDBC,
 * in-memory, or a mock can implement it. The service layer depends
 * only on this contract, not on JDBC specifics.</p>
 *
 * <p><b>Why no {@code update} method?</b> For aggregates with
 * behavior-rich state, persistence is a side effect of {@code save},
 * not a separate operation. We do provide a {@code save} that upserts.</p>
 *
 * @param <T>  the entity type
 * @param <ID> the identity type
 */
public interface Repository<T, ID> {

    /**
     * Persists a new entity or updates an existing one.
     *
     * @return the persisted entity (may have a generated id)
     */
    T save(T entity);

    /**
     * Finds an entity by id.
     *
     * @return an {@code Optional} empty if not found
     */
    Optional<T> findById(ID id);

    /** @return all entities, in an unspecified but stable order. */
    List<T> findAll();

    /**
     * Deletes by id.
     *
     * @return true if an entity was removed, false if it did not exist
     */
    boolean deleteById(ID id);

    /** @return number of stored entities. */
    long count();
}