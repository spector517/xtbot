package com.github.spector517.xtbot.core.repository;

import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class H2ClientRepositoryTest {

    private Path dbDirectory;
    private Path nonExistingDBDirectory;
    private Path regularFile;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        dbDirectory = Files.createTempDirectory("h2");
        nonExistingDBDirectory = Path.of("non", "existing", "db", "directory");
        regularFile = Files.createTempFile("h2", "db");
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        FileUtils.deleteDirectory(dbDirectory.toFile());
        FileUtils.deleteDirectory(Path.of("non").toFile());
    }

    @Test
    @DisplayName("Constructor with valid directory")
    void constructor_0() {
        assertDoesNotThrow(() -> new H2ClientRepository(dbDirectory));
        assertTrue(Files.exists(dbDirectory));
        assertTrue(Files.isDirectory(dbDirectory));
    }

    @Test
    @DisplayName("Constructor with non-existing directory")
    void constructor_1() {
        assertFalse(Files.exists(nonExistingDBDirectory));

        assertDoesNotThrow(() -> new H2ClientRepository(nonExistingDBDirectory));

        assertTrue(Files.exists(nonExistingDBDirectory));
        assertTrue(Files.isDirectory(nonExistingDBDirectory));
    }

    @Test
    @DisplayName("Constructor with regular file")
    void constructor_2() {
        assertThrows(IllegalStateException.class, () -> new H2ClientRepository(regularFile));
    }

    @Test
    @SneakyThrows
    @DisplayName("Client found")
    void findByExternalId_0() {
        var repository = new H2ClientRepository(dbDirectory);
        var clientEntity = getClientEntity().externalId(11L);
        repository.save(clientEntity);

        var foundClient = repository.findByExternalId(11L);

        assertEquals(clientEntity, foundClient);
    }

    @Test
    @DisplayName("Client not found")
    void testFindByExternalId_1() {
        var repository = new H2ClientRepository(dbDirectory);

        assertThrows(
                ClientNotFoundException.class,
                () -> repository.findByExternalId(11L)
        );
    }

    @Test
    @DisplayName("Client updated")
    @SneakyThrows
    void save_0() {
        var repository = new H2ClientRepository(dbDirectory);
        var beforeUpdateClientEntity = getClientEntity()
                .externalId(11L)
                .stages(List.of("test1"));
        repository.save(beforeUpdateClientEntity);
        var updatedClientEntity = getClientEntity()
                .externalId(11L)
                .stages(List.of("test2"));

        repository.save(updatedClientEntity);

        var actualClientEntity = repository.findByExternalId(11L);
        assertEquals(updatedClientEntity, actualClientEntity);
    }

    @Test
    @DisplayName("Client added")
    @SneakyThrows
    void save_1() {
        var repository = new H2ClientRepository(dbDirectory);
        var beforeUpdateClientEntity = getClientEntity()
                .externalId(11L);

        repository.save(beforeUpdateClientEntity);

        var updatedClientEntity = getClientEntity()
                .externalId(22L);

        repository.save(updatedClientEntity);

        assertEquals(beforeUpdateClientEntity, repository.findByExternalId(11L));
        assertEquals(updatedClientEntity, repository.findByExternalId(22L));
    }

    private ClientEntity getClientEntity() {
        return new ClientEntity()
                .externalId(11L)
                .name("testClient")
                .sentMessageIds(List.of(1, 2, 3))
                .stages(List.of("stage1", "stage2"))
                .stageInitiated(true)
                .stageCompleted(false)
                .additionalVars("var1=value1;var2=value2")
                .stageVars("stageVar1=value1;stageVar2=value2");
    }
}