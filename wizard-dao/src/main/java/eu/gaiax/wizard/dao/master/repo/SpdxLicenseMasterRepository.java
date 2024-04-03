/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.dao.master.repo;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import eu.gaiax.wizard.dao.master.entity.SpdxLicenseMaster;
import org.springframework.stereotype.Repository;


/**
 * The interface Standard type master repository.
 */
@Repository
public interface SpdxLicenseMasterRepository extends BaseRepository<SpdxLicenseMaster, String> {

}
