package eu.gaiax.wizard.config.multitenant;

import eu.gaiax.wizard.utils.WizardRestConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Autowired
    @Qualifier(WizardRestConstant.ALL_TENANT_DATASOURCE)
    private Map<Object, Object> dataSourceMap;

    @Bean
    @Primary
    public DataSource getDataSource() {
        CustomRoutingDataSource customDataSource = new CustomRoutingDataSource();
        customDataSource.setTargetDataSources(dataSourceMap);
        customDataSource.afterPropertiesSet();
        customDataSource.setDefaultTargetDataSource(dataSourceMap.get(WizardRestConstant.MASTER_DB_KEY));

        return customDataSource;
    }
}
