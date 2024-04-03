package eu.gaiax.wizard.controller;

import com.smartsensesolutions.java.commons.FilterRequest;
import eu.gaiax.wizard.api.model.CommonResponse;
import eu.gaiax.wizard.api.model.PageResponse;
import eu.gaiax.wizard.api.model.master.CreateTenantRequest;
import eu.gaiax.wizard.api.model.master.TenantDTO;
import eu.gaiax.wizard.api.utils.TenantContext;
import eu.gaiax.wizard.apidocs.MasterResource;
import eu.gaiax.wizard.core.service.master.MasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static eu.gaiax.wizard.utils.WizardRestConstant.API_PUBLIC_CREATE_TENANT;
import static eu.gaiax.wizard.utils.WizardRestConstant.API_PUBLIC_TENANTS;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tenant")
public class MasterController extends BaseController {


    private final MessageSource messageSource;
    private final MasterService masterService;

    @Tag(name = "Tenant")
    @Operation(summary = "Create Tenant")
    @MasterResource.CreateTenantApiDocs
    @PostMapping(path = API_PUBLIC_CREATE_TENANT)
    public CommonResponse<Object> createTenant(@Valid @RequestBody CreateTenantRequest createTenantRequest) {
        TenantContext.setUseMasterDb(true);
        masterService.createTenant(createTenantRequest);
        return CommonResponse.of(messageSource.getMessage("tenant.registered.success", null, LocaleContextHolder.getLocale()));
    }

    @Tag(name = "Tenant")
    @Operation(summary = "Fetch Tenants")
    @PostMapping(path = API_PUBLIC_TENANTS)
    public CommonResponse<PageResponse<TenantDTO>> fetchTenantList(@Valid @RequestBody FilterRequest filterRequest) {
        TenantContext.setUseMasterDb(true);
        return CommonResponse.of(masterService.fetchTenantList(filterRequest));
    }

}
