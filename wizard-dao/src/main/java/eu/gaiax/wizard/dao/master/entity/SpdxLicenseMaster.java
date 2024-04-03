package eu.gaiax.wizard.dao.master.entity;


import eu.gaiax.wizard.dao.tenant.entity.SuperEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "spdx_license_master")
@Getter
@Setter
public class SpdxLicenseMaster extends SuperEntity {

    @Column(name = "license_id")
    private String licenseId;

    @Column(name = "name")
    private String name;

    @Column(name = "reference")
    private String reference;

    @Column(name = "active")
    private Boolean active;
}
