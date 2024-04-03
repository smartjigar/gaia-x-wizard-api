/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.dao.master.repo;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import eu.gaiax.wizard.dao.master.entity.EntityTypeMaster;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * The interface Entity type master repository.
 */
@Repository
public interface EntityTypeMasterRepository extends BaseRepository<EntityTypeMaster, UUID> {

}
