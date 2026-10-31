package com.cmpt276.project.owner.store;

import com.cmpt276.project.owner.model.Salon;

public interface SalonStore {
    Salon findById(String id);
    Salon findByOwnerId(String ownerId);
    void save(Salon salon);
}
