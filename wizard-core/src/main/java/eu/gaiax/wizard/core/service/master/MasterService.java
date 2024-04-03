package eu.gaiax.wizard.core.service.master;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsensesolutions.java.commons.FilterRequest;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.api.model.PageResponse;
import eu.gaiax.wizard.api.model.master.CreateTenantRequest;
import eu.gaiax.wizard.api.model.master.TenantDTO;
import eu.gaiax.wizard.api.utils.Validate;
import eu.gaiax.wizard.dao.TenantConfig;
import eu.gaiax.wizard.dao.TenantDAO;
import eu.gaiax.wizard.dao.master.entity.Tenants;
import eu.gaiax.wizard.dao.master.repo.TenantsRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasterService extends BaseService<Tenants, UUID> {
    private final TenantsRepository tenantsRepository;
    private final SpecificationUtil<Tenants> specificationUtil;
    private final TenantDAO tenantDAO;
    private final ObjectMapper objectMapper;
    private final TenantConfig tenantConfig;

    @Transactional
    @SneakyThrows
    public void createTenant(CreateTenantRequest createTenantRequest) {
        var tenantAlias = tenantsRepository.findByAlias(createTenantRequest.alias());
        Validate.isNotNull(tenantAlias).launch("tenant.alias.already.exits");
        var tenantName = tenantsRepository.findByName(createTenantRequest.name());
        Validate.isNotNull(tenantName).launch("tenant.name.already.exits");

        tenantDAO.createDatabase(createTenantRequest.alias());
        tenantDAO.runLiquibase(createTenantRequest.alias());

        Tenants tenant = Tenants.builder()
                .active(true)
                .name(createTenantRequest.name())
                .alias(createTenantRequest.alias())
                .description(createTenantRequest.description())
                .build();

        tenantsRepository.save(tenant);
    }

    @Override
    protected BaseRepository<Tenants, UUID> getRepository() {
        return tenantsRepository;
    }

    @Override
    protected SpecificationUtil<Tenants> getSpecificationUtil() {
        return specificationUtil;
    }

    public PageResponse<TenantDTO> fetchTenantList(FilterRequest filterRequest) {
        Page<Tenants> pageResponse = filter(filterRequest);
        List<TenantDTO> tenantList = objectMapper.convertValue(pageResponse.getContent(), new TypeReference<>() {
        });
        return PageResponse.of(tenantList, pageResponse, filterRequest.getSort());
    }
}
