package com.github.spector517.xtbot.core.application.repository;

public interface ClientRepository {

    ClientEntity findByExternalId(long externalId) throws ClientNotFoundException;

    void save(ClientEntity entity);
}
