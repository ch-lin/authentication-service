package ch.lin.authentication.service.backend.api.app.config;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.authentication.service.backend.api.app.repository.AuthenticationConfigRepository;
import ch.lin.authentication.service.backend.api.app.repository.ClientRepository;
import ch.lin.authentication.service.backend.api.app.repository.UserRepository;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.Role;
import ch.lin.authentication.service.backend.api.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes the database with default data (Admin User, Clients, Configs).
 * Strictly follows the current database schema.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AuthenticationConfigRepository authConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationDefaultProperties authDefaultProperties;
    private final DefaultConfigFactory defaultConfigFactory;

    // --- Admin User ---
    @Value("${INIT_ADMIN_FIRSTNAME:admin}")
    private String adminFirstname; // Using firstname to store 'admin'

    @Value("${INIT_ADMIN_LASTNAME:admin}")
    private String adminLastname; // Using lastname to store 'admin'

    @Value("${INIT_ADMIN_EMAIL:admin@example.com}")
    private String adminEmail;

    @Value("${INIT_ADMIN_PASSWORD:}")
    private String adminPassword;

    // --- Clients ---
    @Value("${INIT_DOWNLOADER_CLIENT_ID:}")
    private String downloaderClientId;

    @Value("${INIT_DOWNLOADER_CLIENT_SECRET:}")
    private String downloaderClientSecret;

    @Value("${INIT_HUB_CLIENT_ID:}")
    private String hubClientId;

    @Value("${INIT_HUB_CLIENT_SECRET:}")
    private String hubClientSecret;

    // --- Postman Client ---
    @Value("${INIT_POSTMAN_CLIENT_ID:}")
    private String postmanClientId;

    @Value("${INIT_POSTMAN_CLIENT_SECRET:}")
    private String postmanClientSecret;

    @Bean
    @Transactional
    public CommandLineRunner initData() {
        return args -> {
            log.info("🚀 Starting Data Initialization...");

            initAdminUser();
            initDownloaderClient();
            initHubClient();
            initPostmanClient();
            initDefaultConfig();

            log.info("✅ Data Initialization Completed.");
        };
    }

    private void initAdminUser() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠️ Skipping Admin User creation: INIT_ADMIN_PASSWORD is not set or empty.");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        User admin = User.builder()
                .firstname(adminFirstname)
                .lastname(adminLastname)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(Objects.requireNonNull(admin));
        log.info("✅ Created Admin User: {}", adminEmail);
    }

    private void initDownloaderClient() {
        if (downloaderClientId == null || downloaderClientId.isBlank()) {
            log.warn("⚠️ Skipping Downloader Client creation: INIT_DOWNLOADER_CLIENT_ID is not set.");
            return;
        }

        if (clientRepository.findByClientId(downloaderClientId).isPresent()) {
            return;
        }

        Client client = Client.builder()
                .clientName("Downloader Service")
                .clientId(downloaderClientId)
                .clientSecret(passwordEncoder.encode(downloaderClientSecret))
                .role(Role.SERVICE)
                .build();

        clientRepository.save(Objects.requireNonNull(client));
        log.info("✅ Created Downloader Client: {}", downloaderClientId);
    }

    private void initHubClient() {
        if (hubClientId == null || hubClientId.isBlank()) {
            log.warn("⚠️ Skipping Hub Client creation: INIT_HUB_CLIENT_ID is not set.");
            return;
        }

        if (clientRepository.findByClientId(hubClientId).isPresent()) {
            return;
        }

        Client client = Client.builder()
                .clientName("YouTube Hub Web")
                .clientId(hubClientId)
                .clientSecret(passwordEncoder.encode(hubClientSecret))
                .role(Role.SERVICE)
                // redirectUris is omitted as requested
                .build();

        clientRepository.save(Objects.requireNonNull(client));
        log.info("✅ Created Hub Client: {}", hubClientId);
    }

    private void initPostmanClient() {
        if (postmanClientId == null || postmanClientId.isBlank()) {
            log.warn("⚠️ Skipping Postman Client creation: INIT_POSTMAN_CLIENT_ID is not set.");
            return;
        }

        if (clientRepository.findByClientId(postmanClientId).isPresent()) {
            log.info("Postman Client ({}) already exists.", postmanClientId);
            return;
        }

        Client client = Client.builder()
                .clientName("Postman Test Client")
                .clientId(postmanClientId)
                .clientSecret(passwordEncoder.encode(postmanClientSecret))
                .role(Role.ADMIN)
                //.redirectUris(postmanRedirectUris)
                .build();

        clientRepository.save(Objects.requireNonNull(client));
        log.info("✅ Created Postman Client: {}", postmanClientId);
    }

    private void initDefaultConfig() {
        String configName = authDefaultProperties.getName();
        if (authConfigRepository.findByName(configName).isPresent()) {
            return;
        }

        AuthenticationConfig config = defaultConfigFactory.create(authDefaultProperties);

        authConfigRepository.save(Objects.requireNonNull(config));
        log.info("✅ Created Default Authentication Config.");
    }
}
