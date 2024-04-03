package eu.gaiax.wizard.core.service.data_master;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.dao.master.entity.AccessTypeMaster;
import eu.gaiax.wizard.dao.master.repo.AccessTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service(value = "accessTypeMasterService")
@RequiredArgsConstructor
public class AccessTypeMasterService extends BaseService<AccessTypeMaster, String> {

    private final SpecificationUtil<AccessTypeMaster> specificationUtil;

    private final AccessTypeMasterRepository accessTypeMasterRepository;

    @Override
    protected BaseRepository<AccessTypeMaster, String> getRepository() {
        return accessTypeMasterRepository;
    }

    @Override
    protected SpecificationUtil<AccessTypeMaster> getSpecificationUtil() {
        return specificationUtil;
    }
}
