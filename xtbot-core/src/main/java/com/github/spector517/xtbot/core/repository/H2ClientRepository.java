package com.github.spector517.xtbot.core.repository;

import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.core.repository.entity.MessageEntity;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class H2ClientRepository implements ClientRepository {

    private static final String DB_NAME = "xtbot";

    private final SessionFactory sessionFactory;

    @SneakyThrows
    public H2ClientRepository(Path directory) {
        directory = directory.toAbsolutePath().normalize();
        var dbFile = directory.resolve(DB_NAME);
        if (!Files.exists(directory)) {
            log.warn("H2 database location {} does not exist, creating a new one", directory);
            Files.createDirectories(directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("H2 database location must be a directory, but was: %s"
                    .formatted(directory));
        }
        var configuration = new Configuration();
        configuration.setProperties(getH2Properties(dbFile.toString()));
        configuration.addAnnotatedClass(ClientEntity.class);
        configuration.addAnnotatedClass(MessageEntity.class);

        this.sessionFactory = configuration
                .buildSessionFactory(
                        new StandardServiceRegistryBuilder()
                                .applySettings(configuration.getProperties())
                                .build()
                );
    }

    @Override
    public synchronized ClientEntity findByExternalId(long externalId) throws ClientNotFoundException {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var client = getClientByExternalId(externalId, session);
            if (client.isEmpty()) {
                transaction.commit();
                throw new ClientNotFoundException("Client with externalId %d not found".formatted(externalId));
            }
            transaction.commit();
            return client.get();
        }
    }

    @Override
    public synchronized void save(ClientEntity entity) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var existingClient = getClientByExternalId(entity.externalId(), session);
            if (existingClient.isEmpty()) {
                log.debug("Saving new client with externalId {}", entity.externalId());
                // Set client reference on new messages and persist via cascade
                if (entity.messages() != null) {
                    entity.messages().forEach(m -> m.client(entity));
                }
                session.persist(entity);
            } else {
                log.debug("Updating existing client with externalId {}", entity.externalId());
                var managed = existingClient.get();

                // Determine which messages are new (by telegramMessageId)
                Set<Integer> existingTelegramIds = managed.messages() != null
                        ? managed.messages().stream()
                                .map(MessageEntity::telegramMessageId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                        : Set.of();

                List<MessageEntity> newMessages = entity.messages() != null
                        ? entity.messages().stream()
                                .filter(m -> m.telegramMessageId() == null
                                        || !existingTelegramIds.contains(m.telegramMessageId()))
                                .toList()
                        : List.of();

                // Persist only new messages, using the managed entity as client reference
                newMessages.forEach(m -> {
                    m.client(managed);
                    session.persist(m);
                });

                // Merge client fields without touching the messages collection
                entity.id(managed.id());
                entity.messages(managed.messages());
                session.merge(entity);
            }
            transaction.commit();
        }
    }

    private Properties getH2Properties(String dbFileLocation) {
        var props = new Properties();
        props.put("hibernate.connection.driver_class", "org.h2.Driver");
        props.put("hibernate.connection.url", "jdbc:h2:file:%s".formatted(dbFileLocation));
        props.put("hibernate.connection.username", "sa");
        props.put("hibernate.connection.password", "");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.hbm2ddl.auto", "create-only");
        return props;
    }

    private Optional<ClientEntity> getClientByExternalId(long externalId, Session session) {
        return session.createQuery("FROM ClientEntity WHERE externalId = :externalId", ClientEntity.class)
                .setParameter("externalId", externalId)
                .uniqueResultOptional();
    }
}
