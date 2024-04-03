package eu.gaiax.wizard.api.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ParticipantRegisterRequest(@Email(message = "email.required") String email,
                                         @NotBlank(message = "tenant.not.found") String tenantAlias,
                                         @Valid ParticipantOnboardRequest onboardRequest) {
}
