package eu.gaiax.wizard.core.service.data_master;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.api.model.LabelLevelTypeInterface;
import eu.gaiax.wizard.dao.master.entity.LabelLevelTypeMaster;
import eu.gaiax.wizard.dao.master.repo.LabelLevelTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelLevelService extends BaseService<LabelLevelTypeMaster, String> {

    private final SpecificationUtil<LabelLevelTypeMaster> specificationUtil;

    private final LabelLevelTypeMasterRepository labelLevelTypeMasterRepository;

    @Override
    protected BaseRepository<LabelLevelTypeMaster, String> getRepository() {
        return labelLevelTypeMasterRepository;
    }

    @Override
    protected SpecificationUtil<LabelLevelTypeMaster> getSpecificationUtil() {
        return specificationUtil;
    }

    public List<LabelLevelTypeInterface> getLabelLevelTypeAndQuestionList() {
        return labelLevelTypeMasterRepository.findAllByActiveIsTrueAndLabelLevelQuestionMasterListIsNotNull();
    }
}
