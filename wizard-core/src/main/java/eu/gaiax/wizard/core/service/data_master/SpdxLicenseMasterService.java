package eu.gaiax.wizard.core.service.data_master;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.dao.master.entity.SpdxLicenseMaster;
import eu.gaiax.wizard.dao.master.repo.SpdxLicenseMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service(value = "spdxLicenseTypeMasterService")
@RequiredArgsConstructor
public class SpdxLicenseMasterService extends BaseService<SpdxLicenseMaster, String> {

    private final SpecificationUtil<SpdxLicenseMaster> specificationUtil;

    private final SpdxLicenseMasterRepository spdxLicenseMasterRepository;

    @Override
    protected BaseRepository<SpdxLicenseMaster, String> getRepository() {
        return spdxLicenseMasterRepository;
    }

    @Override
    protected SpecificationUtil<SpdxLicenseMaster> getSpecificationUtil() {
        return specificationUtil;
    }

}
