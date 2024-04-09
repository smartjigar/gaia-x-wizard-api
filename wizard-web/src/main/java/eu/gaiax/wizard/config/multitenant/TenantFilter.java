package eu.gaiax.wizard.config.multitenant;

import eu.gaiax.wizard.api.utils.TenantContext;
import eu.gaiax.wizard.dao.TenantDAO;
import eu.gaiax.wizard.utils.WizardRestConstant;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Order(1)
@Slf4j
public class TenantFilter implements Filter {

    @Autowired
    private TenantDAO tenantDAO;
    @Autowired
    @Qualifier(WizardRestConstant.ALL_TENANT_DATASOURCE)
    private Map<Object, Object> dataSourceMap;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        String tenantName = null;
        String token = req.getHeader("Authorization");
        String headerTenant = req.getHeader(WizardRestConstant.TENANT_HEADER_KEY);
        if (token != null) {
            Jwt credentials = (Jwt) SecurityContextHolder.getContext().getAuthentication().getCredentials();
            Map<String, Object> claims = credentials.getClaims();
            tenantName = (String) claims.get("tenant");
        }
        if (tenantName == null && headerTenant != null) {
            tenantName = headerTenant;
        }
        if (!dataSourceMap.containsKey(tenantName)) {
            dataSourceMap.put(tenantName, tenantDAO.getTenantDataSource(tenantName));
        }
        TenantContext.setCurrentTenant(tenantName);
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
