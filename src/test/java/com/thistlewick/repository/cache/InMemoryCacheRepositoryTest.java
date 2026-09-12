package com.thistlewick.repository.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCacheRepositoryTest {

    /** Minimal entity for testing. */
    record Item(Long id, String name) {}

    private InMemoryCacheRepository<Item, Long> cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryCacheRepository<>(Item::id);
    }

    @Test
    @DisplayName("save and findById round-trip")
    void saveAndFind() {
        Item item = new Item(1L, "alpha");
        cache.save(item);

        assertEquals(item, cache.findById(1L).orElseThrow());
        assertEquals(1, cache.count());
    }

    @Test
    @DisplayName("findById increments misses on absent id")
    void missIncrementsCounter() {
        assertTrue(cache.findById(99L).isEmpty());
        assertEquals(1, cache.missCount());
        assertEquals(0, cache.hitCount());
    }

    @Test
    @DisplayName("deleteById removes entity")
    void deleteWorks() {
        cache.save(new Item(1L, "a"));
        assertTrue(cache.deleteById(1L));
        assertFalse(cache.deleteById(1L));   // already gone
        assertEquals(0, cache.count());
    }

    @Test
    @DisplayName("save rejects null id")
    void rejectsNullId() {
        assertThrows(IllegalArgumentException.class,
                () -> cache.save(new Item(null, "x")));
    }

    @Test
    @DisplayName("findAll returns everything")
    void findAllWorks() {
        cache.save(new Item(1L, "a"));
        cache.save(new Item(2L, "b"));
        assertEquals(2, cache.findAll().size());
    }
}