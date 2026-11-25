package com.cmpt276.project.owner.store;

import com.cmpt276.project.owner.model.Salon;
import java.util.List;
import java.util.function.Function;

public interface SalonStore {
    // Return all saved salons, or an empty list when none have been created.
    List<Salon> findAll();
    Salon findById(String id);
    Salon findByOwnerId(String ownerId);
    default <T> T withSalonForUpdate(String ownerId, Function<Salon, T> edit) {
        synchronized (this) {
            return edit.apply(findByOwnerId(ownerId));
        }
    }

    void save(Salon salon);
    Salon findBySlug(String slug);
}
