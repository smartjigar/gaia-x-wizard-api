package eu.gaiax.wizard.config.multitenant;

import eu.gaiax.wizard.dao.TenantDAO;
import eu.gaiax.wizard.dao.master.entity.Tenants;
import eu.gaiax.wizard.dao.master.repo.TenantsRepository;
import eu.gaiax.wizard.utils.WizardRestConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantInitializer {

    private final TenantDAO tenantDAO;

    @Bean(name = WizardRestConstant.ALL_TENANT_DATASOURCE)
    @Qualifier(WizardRestConstant.ALL_TENANT_DATASOURCE)
    public Map<Object, Object> allTenantDbConfig() {
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(WizardRestConstant.MASTER_DB_KEY, tenantDAO.getMasterDataSource());
        return dataSourceMap;
    }

    private void addTenantDataSource(Map<Object, Object> globalDatasource, ApplicationContext context) {
        TenantsRepository tenantsRepository = (TenantsRepository) context.getBean("tenantsRepository");
        List<Tenants> allTenants = tenantsRepository.findAll();
        allTenants.forEach(e -> globalDatasource.put(e.getAlias(), tenantDAO.getTenantDataSource(e.getAlias())));
    }


}
