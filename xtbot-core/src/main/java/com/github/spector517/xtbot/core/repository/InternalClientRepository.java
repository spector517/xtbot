package com.github.spector517.xtbot.core.repository;

import java.util.ArrayList;
import java.util.List;

import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientNotFoundException;
import com.github.spector517.xtbot.core.application.repository.ClientRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalClientRepository implements ClientRepository {

    private final List<ClientEntity> clients;

    public InternalClientRepository() {
        this.clients = new ArrayList<>();
    }

    @Override
    public synchronized ClientEntity findByExternalId(long externalId) throws ClientNotFoundException {
        var index = indexByExternalId(externalId);
        if (index != -1) {
            return clients.get(index);
        }
        var msg = "Client with external id '%s' not found in repository".formatted(externalId);
        log.debug(msg);
        throw new ClientNotFoundException(msg);
    }

    @Override
    public synchronized void save(ClientEntity entity) {
        var index = indexByExternalId(entity.externalId());
        if (index != -1) {
            log.debug("Save existing client with id '{}' to repository", entity.externalId());
            clients.set(index, entity);
        } else {
            var newId = clients.size() + 1L;
            log.debug("Save new client with id '{}' to repository", newId);
            entity.id(newId);
            clients.add(entity);
        }
    }

    private int indexByExternalId(long externalId) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).externalId() == externalId) {
                return i;
            }
        }
        return -1;
    }
}
