package com.github.spector517.xtbot.core.repository;

import com.github.spector517.xtbot.core.repository.entity.ClientEntity;

public interface ClientRepository {

    ClientEntity findByExternalId(long externalId) throws ClientNotFoundException;

    void save(ClientEntity entity);
}
