package eu.gaiax.wizard.dao.master.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.gaiax.wizard.dao.tenant.entity.SuperEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "tenants")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Tenants extends SuperEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "alias", nullable = false, unique = true)
    private String alias;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Override
    @JsonIgnore(value = false)
    @JsonProperty
    public Date getCreatedAt() {
        return super.getCreatedAt();
    }
}
