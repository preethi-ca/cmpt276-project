package com.cmpt276.project.owner.store;

import com.cmpt276.project.owner.model.Salon;
import java.util.List;

public interface SalonStore {
    // Return all saved salons, or an empty list when none have been created.
    List<Salon> findAll();
    Salon findById(String id);
    Salon findByOwnerId(String ownerId);
    void save(Salon salon);
    Salon findBySlug(String slug);
}
