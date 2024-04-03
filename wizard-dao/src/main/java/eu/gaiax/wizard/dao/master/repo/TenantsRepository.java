package eu.gaiax.wizard.dao.master.repo;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import eu.gaiax.wizard.dao.master.entity.Tenants;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantsRepository extends BaseRepository<Tenants, UUID> {
    Tenants findByAlias(String alias);

    Tenants findByName(String name);

    List<Tenants> findByActiveTrue();
}
