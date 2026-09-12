package com.thistlewick.repository.cache;

import com.thistlewick.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic in-memory {@link Repository} backed by a {@link ConcurrentHashMap}.
 *
 * <h2>Purpose</h2>
 * <p>Used as a fast, thread-safe cache — e.g. "tasks due today" that
 * would otherwise require a DB round-trip on every CLI redraw.</p>
 *
 * <h2>Generics</h2>
 * <p>Works for any {@code T} that carries an id extractor. The caller
 * supplies a {@link IdExtractor} so this class doesn't depend on any
 * specific entity.</p>
 *
 * @param <T>  entity type
 * @param <ID> identity type
 */
public class InMemoryCacheRepository<T, ID> implements Repository<T, ID> {

    /**
     * Strategy for extracting the id from an entity.
     * Keeps this cache generic and decoupled from domain types.
     */
    @FunctionalInterface
    public interface IdExtractor<T, ID> {
        ID extract(T entity);
    }

    private final Map<ID, T> store = new ConcurrentHashMap<>();
    private final IdExtractor<T, ID> idExtractor;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public InMemoryCacheRepository(IdExtractor<T, ID> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor required");
    }

    @Override
    public T save(T entity) {
        ID id = idExtractor.extract(entity);
        if (id == null) {
            throw new IllegalArgumentException("entity id must not be null");
        }
        store.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        T value = store.get(id);
        if (value == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        hits.incrementAndGet();
        return Optional.of(value);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean deleteById(ID id) {
        return store.remove(id) != null;
    }

    @Override
    public long count() {
        return store.size();
    }

    /** Clears the entire cache. Useful between CLI sessions or tests. */
    public void clear() {
        store.clear();
    }

    /** @return number of successful cache lookups since construction. */
    public long hitCount() {
        return hits.get();
    }

    /** @return number of failed cache lookups since construction. */
    public long missCount() {
        return misses.get();
    }
}