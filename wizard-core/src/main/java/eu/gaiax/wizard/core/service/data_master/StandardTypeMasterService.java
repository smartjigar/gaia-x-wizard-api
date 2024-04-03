package eu.gaiax.wizard.core.service.data_master;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.dao.master.entity.StandardTypeMaster;
import eu.gaiax.wizard.dao.master.repo.StandardTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(value = "standardTypeMasterService")
@RequiredArgsConstructor
public class StandardTypeMasterService extends BaseService<StandardTypeMaster, String> {

    private final SpecificationUtil<StandardTypeMaster> specificationUtil;

    private final StandardTypeMasterRepository standardTypeMasterRepository;

    @Override
    protected BaseRepository<StandardTypeMaster, String> getRepository() {
        return standardTypeMasterRepository;
    }

    @Override
    protected SpecificationUtil<StandardTypeMaster> getSpecificationUtil() {
        return specificationUtil;
    }

    public List<StandardTypeMaster> findAllByTypeIn(List<String> standardNameList) {
        return standardTypeMasterRepository.findAllByTypeIn(standardNameList);
    }
}
