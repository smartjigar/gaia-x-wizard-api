package eu.gaiax.wizard.api.model.master;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record CreateTenantRequest(@NotEmpty(message = "name.required") String name,
                                  @NotEmpty(message = "alias.required")
                                  @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "invalid.alias.format") String alias,
                                  String description) {
}
