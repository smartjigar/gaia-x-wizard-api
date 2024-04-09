package eu.gaiax.wizard.core.service.keycloak;


import eu.gaiax.wizard.api.exception.BadDataException;
import eu.gaiax.wizard.api.model.KeycloakRequiredActionsEnum;
import eu.gaiax.wizard.api.model.setting.KeycloakSettings;
import eu.gaiax.wizard.api.utils.RoleConstant;
import eu.gaiax.wizard.api.utils.StringPool;
import eu.gaiax.wizard.api.utils.Validate;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final KeycloakSettings keycloakSettings;

    protected Keycloak getKeycloak() {
        return KeycloakBuilder.builder()
                .clientId(keycloakSettings.clientId())
                .clientSecret(keycloakSettings.clientSecret())
                .realm(keycloakSettings.realm())
                .serverUrl(keycloakSettings.authServer())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

    protected RealmResource getRealmResource() {
        Keycloak keycloak = getKeycloak();
        return keycloak.realm(keycloakSettings.realm());
    }

    public void createParticipantUser(String id, String legalName, String email, String tenant) {
        addUser(id, legalName, email, tenant);
        addClientRole(email, RoleConstant.PARTICIPANT_ROLE);
        sendRequiredActionsEmail(email);
    }

    private void addUser(String id, String legalName, String email, String tenant) {
        UserRepresentation existingUser = getKeycloakUserByEmail(email);
        if (existingUser != null) {
            deleteExistingUser(existingUser);
        }

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setEmail(email);
        userRepresentation.setFirstName(legalName);
        userRepresentation.setUsername(email);

        Map<String, List<String>> userAttributes = new HashMap<>();
        userAttributes.put(StringPool.ID, Collections.singletonList(id));
        userAttributes.put(StringPool.TENANT, Collections.singletonList(tenant));
        userRepresentation.setAttributes(userAttributes);

        RealmResource realmResource = getRealmResource();
        UsersResource usersResource = realmResource.users();

        try (Response response = usersResource.create(userRepresentation)) {
            log.info("Keycloak User Creation status: {}", response.getStatus());
            if (response.getStatus() != HttpStatus.CREATED.value()) {
                throw new BadDataException("keycloak.create.user.failed");
            }
        }

        log.info("keycloak user created");
    }

    private void deleteExistingUser(UserRepresentation userRepresentation) {
        UsersResource usersResource = getRealmResource().users();
        try (Response ignored = usersResource.delete(userRepresentation.getId())) {
            log.info("Deleting existing user with email: {}", userRepresentation.getEmail());
        }
    }

    public void sendRequiredActionsEmail(String email) {
        UserRepresentation userRepresentation = getKeycloakUserByEmail(email);
        Validate.isNull(userRepresentation).launch(new BadDataException("User not found"));

        UserResource userResource = getRealmResource().users().get(userRepresentation.getId());
        userResource.executeActionsEmail(
                keycloakSettings.publicClientId(),
                keycloakSettings.requiredActionsEmailRedirectionUrl(),
                keycloakSettings.actionTokenLifespan(),
                List.of(KeycloakRequiredActionsEnum.WEBAUTHN_REGISTER_PASSWORDLESS.getValue())
        );

        log.info("Required actions email sent to the user");
    }

    public void addClientRole(String email, String role) {
        UserRepresentation userRepresentation = getKeycloakUserByEmail(email);
        Validate.isNull(userRepresentation).launch(new BadDataException("User not found"));

        List<ClientRepresentation> clientRepresentationList = getRealmResource().clients().findByClientId(keycloakSettings.clientId());
        Validate.isNull(clientRepresentationList).launch(new BadDataException("Keycloak client not found"));

        ClientRepresentation keycloakClient = clientRepresentationList.get(0);
        ClientResource clientResource = getRealmResource().clients().get(keycloakClient.getId());
        RoleResource participantRole = clientResource.roles().get(role);

        UserResource userResource = getRealmResource().users().get(userRepresentation.getId());
        userResource.roles().clientLevel(keycloakClient.getId()).add(Collections.singletonList(participantRole.toRepresentation()));

        log.info("client role added to keycloak user");
    }

    public UserRepresentation getKeycloakUserByEmail(String email) {
        log.debug("getKeycloakUserByEmail: email={}", email);

        RealmResource realmResource = getRealmResource();
        UsersResource usersResource = realmResource.users();
        List<UserRepresentation> users = usersResource.searchByEmail(email, true);
        if (CollectionUtils.isEmpty(users)) {
            return null;
        } else {
            getRealmResource().users().get(users.get(0).getId());
            return users.get(0);
        }
    }

    public String fetchTenantFromParticipantId(String participantId) {
        String attQuery = "id:" + participantId;
        UserRepresentation userByAttribute = getUserByAttribute(attQuery);
        List<String> tenant = userByAttribute.getAttributes().getOrDefault("tenant", null);
        if (tenant != null) {
            return tenant.get(0);
        } else {
            throw new BadDataException("tenant.not.found");
        }
    }

    public UserRepresentation getUserByAttribute(String attributeValue) {
        RealmResource realmResource = getRealmResource();
        UsersResource usersResource = realmResource.users();
        List<UserRepresentation> userRepresentations = usersResource.searchByAttributes(attributeValue);//q=key:value
        if (CollectionUtils.isEmpty(userRepresentations)) {
            return null;
        } else {
            return userRepresentations.get(0);
        }
    }

    public Boolean isLoginDeviceConfigured(String email) {
        try {
            UserResource userResource = getRealmResource().users().get(getKeycloakUserByEmail(email).getId());
            return userResource.credentials().stream().anyMatch(credentialRepresentation -> credentialRepresentation.getType().equals(StringPool.WEBAUTHN_PASSWORDLESS));
        } catch (Exception e) {
            log.error("Error while fetching user credential list: ", e);
            return false;
        }
    }
}
