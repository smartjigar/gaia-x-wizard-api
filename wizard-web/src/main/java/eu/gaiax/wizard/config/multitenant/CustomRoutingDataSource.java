package eu.gaiax.wizard.config.multitenant;

import eu.gaiax.wizard.api.utils.TenantContext;
import eu.gaiax.wizard.utils.WizardRestConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        afterPropertiesSet();
        String currentTenant = TenantContext.getCurrentTenant();
        Boolean useMasterDb = TenantContext.getUseMasterDb();
        if (StringUtils.isEmpty(currentTenant) || useMasterDb) {
            return WizardRestConstant.MASTER_DB_KEY;
        }
        return currentTenant;
    }
}
