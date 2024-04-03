package eu.gaiax.wizard.dao;

import eu.gaiax.wizard.dao.master.entity.Tenants;
import eu.gaiax.wizard.dao.master.repo.TenantsRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantConfig {

    private final TenantsRepository tenantsRepository;
    @Getter
    private final Map<Object, Tenants> tenantMap;

    public TenantConfig(TenantsRepository tenantsRepository) {
        this.tenantsRepository = tenantsRepository;
        tenantMap = new HashMap<>();
        tenantList();
    }

    public void tenantList() {
        List<Tenants> ls = tenantsRepository.findByActiveTrue();
        ls.forEach(e -> tenantMap.put(e.getAlias(), e));
    }


}
