/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.dao.master.repo;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import eu.gaiax.wizard.dao.master.entity.RequestTypeMaster;
import org.springframework.stereotype.Repository;


/**
 * The interface Request type master repository.
 */
@Repository
public interface RequestTypeMasterRepository extends BaseRepository<RequestTypeMaster, String> {

}
