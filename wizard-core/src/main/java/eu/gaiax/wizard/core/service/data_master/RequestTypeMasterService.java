package eu.gaiax.wizard.core.service.data_master;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.dao.master.entity.RequestTypeMaster;
import eu.gaiax.wizard.dao.master.repo.RequestTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service(value = "requestTypeMasterService")
@RequiredArgsConstructor
public class RequestTypeMasterService extends BaseService<RequestTypeMaster, String> {

    private final SpecificationUtil<RequestTypeMaster> specificationUtil;

    private final RequestTypeMasterRepository registrationTypeMasterRepository;

    @Override
    protected BaseRepository<RequestTypeMaster, String> getRepository() {
        return registrationTypeMasterRepository;
    }

    @Override
    protected SpecificationUtil<RequestTypeMaster> getSpecificationUtil() {
        return specificationUtil;
    }
}
