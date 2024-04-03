/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.dao.master.repo;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import eu.gaiax.wizard.dao.master.entity.AccessTypeMaster;
import org.springframework.stereotype.Repository;


/**
 * The interface Access type master repository.
 */
@Repository
public interface AccessTypeMasterRepository extends BaseRepository<AccessTypeMaster, String> {

}
