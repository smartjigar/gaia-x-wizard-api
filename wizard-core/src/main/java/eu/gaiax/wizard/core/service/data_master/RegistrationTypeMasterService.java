package eu.gaiax.wizard.core.service.data_master;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.dao.master.entity.RegistrationTypeMaster;
import eu.gaiax.wizard.dao.master.repo.RegistrationTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service(value = "registrationTypeMasterService")
@RequiredArgsConstructor
public class RegistrationTypeMasterService extends BaseService<RegistrationTypeMaster, String> {

    private final SpecificationUtil<RegistrationTypeMaster> specificationUtil;

    private final RegistrationTypeMasterRepository registrationTypeMasterRepository;

    @Override
    protected BaseRepository<RegistrationTypeMaster, String> getRepository() {
        return registrationTypeMasterRepository;
    }

    @Override
    protected SpecificationUtil<RegistrationTypeMaster> getSpecificationUtil() {
        return specificationUtil;
    }
}
