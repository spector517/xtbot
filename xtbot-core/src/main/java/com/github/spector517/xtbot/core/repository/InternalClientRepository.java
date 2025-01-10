package com.github.spector517.xtbot.core.repository;

import java.util.ArrayList;
import java.util.List;

import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientNotFoundException;
import com.github.spector517.xtbot.core.application.repository.ClientRepository;

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
        throw new ClientNotFoundException("Client with external id '%s' not found".formatted(externalId));
    }

    @Override
    public synchronized void save(ClientEntity entity) {
        var index = indexByExternalId(entity.externalId());
        if (index != -1) {
            clients.set(index, entity);
        } else {
            entity.id(clients.size() + 1L);
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
