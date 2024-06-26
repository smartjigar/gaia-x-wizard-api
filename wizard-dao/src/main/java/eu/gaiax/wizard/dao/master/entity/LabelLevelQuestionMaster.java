package eu.gaiax.wizard.dao.master.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import eu.gaiax.wizard.api.model.ApplicableLevelCriterionEnum;
import eu.gaiax.wizard.dao.tenant.entity.SuperEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "label_level_question_master")
@Getter
@Setter
@NoArgsConstructor
public class LabelLevelQuestionMaster extends SuperEntity {

    @Column(name = "type_id", insertable = false, updatable = false)
    private UUID typeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
    @JsonBackReference
    private LabelLevelTypeMaster type;

    @Column(name = "criterion_number")
    private String criterionNumber;

    @Column(name = "question")
    private String question;

    @Column(name = "basic_conformity")
    @Enumerated(EnumType.STRING)
    private ApplicableLevelCriterionEnum basicConformity;

    @Column(name = "level_1")
    @Enumerated(EnumType.STRING)
    private ApplicableLevelCriterionEnum level1;

    @Column(name = "highest_label_level")
    private String highestLabelLevel;

    @Column(name = "active")
    private boolean active;

    public LabelLevelQuestionMaster(UUID id, String criterionNumber, String question, String highestLabelLevel) {
        super(id);
        this.criterionNumber = criterionNumber;
        this.question = question;
        this.highestLabelLevel = highestLabelLevel;
    }
}
