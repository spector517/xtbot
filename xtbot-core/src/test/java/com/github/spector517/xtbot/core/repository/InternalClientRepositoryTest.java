package com.github.spector517.xtbot.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientNotFoundException;

import lombok.SneakyThrows;

class InternalClientRepositoryTest {

    private final long externalId = 11L;

    private ClientEntity clientEntity;
    private ClientEntity updatedClientEntity;

    @BeforeEach
    void setUp() {
        clientEntity = new ClientEntity();
        updatedClientEntity = new ClientEntity();
    }

    @Test
    @SneakyThrows
    @DisplayName("Client found")
    void testFindByExternalId_0() {
        var repository = new InternalClientRepository();
        clientEntity.externalId(externalId);
        repository.save(clientEntity);

        var foundClient = repository.findByExternalId(externalId);

        assertEquals(clientEntity, foundClient);
    }

    @Test
    @DisplayName("Client not found")
    void testFindByExternalId_1() {
        var repository = new InternalClientRepository();

        assertThrows(
            ClientNotFoundException.class,
            () -> repository.findByExternalId(externalId)
        );
    }

    @Test
    @DisplayName("Client updated")
    @SneakyThrows
    void testSave_0() {
        var repository = new InternalClientRepository();
        var id = 1L;
        clientEntity.currentStage("test1");
        clientEntity.id(id);
        clientEntity.externalId(externalId);
        repository.save(clientEntity);
        updatedClientEntity.currentStage("test2");
        updatedClientEntity.id(id);
        updatedClientEntity.externalId(externalId);

        repository.save(updatedClientEntity);

        var actualClientEntity = repository.findByExternalId(externalId);
        assertEquals(updatedClientEntity, actualClientEntity);
    }

    @Test
    @DisplayName("Client added")
    @SneakyThrows
    void testSave_1() {
        var repository = new InternalClientRepository();
        clientEntity.externalId(externalId);
        repository.save(clientEntity);
        long updatedClientExternalId = 22L;
        updatedClientEntity.externalId(updatedClientExternalId);

        repository.save(updatedClientEntity);

        var actualClientEntity = repository.findByExternalId(updatedClientExternalId);
        assertEquals(2L, actualClientEntity.id());
        assertEquals(updatedClientEntity, actualClientEntity);
    }
}
