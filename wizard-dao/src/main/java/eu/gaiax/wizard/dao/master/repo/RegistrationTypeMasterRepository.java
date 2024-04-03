/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.dao.master.repo;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import eu.gaiax.wizard.dao.master.entity.RegistrationTypeMaster;
import org.springframework.stereotype.Repository;

/**
 * The interface Registration type master repository.
 */
@Repository
public interface RegistrationTypeMasterRepository extends BaseRepository<RegistrationTypeMaster, String> {

}
