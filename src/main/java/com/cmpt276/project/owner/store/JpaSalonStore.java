
package com.cmpt276.project.owner.store;

import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.model.OpeningDay;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("postgres")
public class JpaSalonStore implements SalonStore {

    private final SalonJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaSalonStore(
            SalonJpaRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Salon findById(String id) {
        return repository.findById(id)
                .map(this::toSalon)
                .orElse(null);
    }

    @Override
    public Salon findByOwnerId(String ownerId) {
        return repository.findByOwnerId(ownerId)
                .map(this::toSalon)
                .orElse(null);
    }

    @Override
    public Salon findBySlug(String slug) {
        return repository.findBySlug(slug)
                .map(this::toSalon)
                .orElse(null);
    }

    @Override
    public void save(Salon salon) {
        SalonEntity entity = new SalonEntity();

        entity.setId(salon.getId());
        entity.setOwnerId(salon.getOwnerId());
        entity.setName(salon.getName());
        entity.setSlug(salon.getSlug());
        entity.setDescription(salon.getDescription());
        entity.setAddress(salon.getAddress());

        try {
            entity.setServicesJson(
                    objectMapper.writeValueAsString(
                            salon.getServices()));

            entity.setOpeningHoursJson(
                    objectMapper.writeValueAsString(
                            salon.getOpeningHours()));

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to serialize salon data", e);
        }

        repository.save(entity);
    }

    private Salon toSalon(SalonEntity entity) {
        Salon salon = new Salon();

        salon.setId(entity.getId());
        salon.setOwnerId(entity.getOwnerId());
        salon.setName(entity.getName());
        salon.setSlug(entity.getSlug());
        salon.setDescription(entity.getDescription());
        salon.setAddress(entity.getAddress());

        try {
            List<SalonService> services =
                    entity.getServicesJson() == null
                    ? new ArrayList<>()
                    : objectMapper.readValue(
                            entity.getServicesJson(),
                            new TypeReference<List<SalonService>>() {});

            List<OpeningDay> hours =
                    entity.getOpeningHoursJson() == null
                    ? new ArrayList<>()
                    : objectMapper.readValue(
                            entity.getOpeningHoursJson(),
                            new TypeReference<List<OpeningDay>>() {});

            salon.setServices(services);
            salon.setOpeningHours(hours);

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to deserialize salon data", e);
        }

        return salon;
    }
}