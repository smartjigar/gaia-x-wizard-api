package eu.gaiax.wizard.api.model.master;

import java.util.Date;

public record TenantDTO(String id, String name, String description, String alias, Date createdAt) {
}
