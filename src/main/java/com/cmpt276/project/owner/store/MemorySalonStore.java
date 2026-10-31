package com.cmpt276.project.owner.store;

import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.store.SalonStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("demo")
public class MemorySalonStore implements SalonStore {
    private final Map<String, Salon> salons = new LinkedHashMap<>();

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

    public synchronized void save(Salon salon) {
        // 新 ID 会添加条目，已有 ID 会更新条目。此处保存的是对象引用，不是数据库记录或副本。
        salons.put(salon.getId(), salon);
    }
}
