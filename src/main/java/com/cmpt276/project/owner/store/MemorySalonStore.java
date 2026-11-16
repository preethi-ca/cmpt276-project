package com.cmpt276.project.owner.store;

import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.store.SalonStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class MemorySalonStore implements SalonStore {
    private final Map<String, Salon> salons = new LinkedHashMap<>();

    @Override
    public synchronized List<Salon> findAll() {
        // Copy the collection so callers cannot remove entries from the backing map.
        return new ArrayList<>(salons.values());
    }

    public synchronized Salon findById(String id) {
        return salons.get(id);
    }

    public synchronized Salon findByOwnerId(String ownerId) {
        for (Salon salon : salons.values()) {
            if (salon.getOwnerId().equals(ownerId)) {
                return salon;
            }
        }
        return null;
    }

    public synchronized Salon findBySlug(String slug) {
        for (Salon salon : salons.values()) {
            if (slug.equals(salon.getSlug())) {
                return salon;
            }
        }
        return null;
    }

    public synchronized void save(Salon salon) {
        salons.put(salon.getId(), salon);
    }
}
