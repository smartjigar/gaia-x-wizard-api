package eu.gaiax.wizard.dao.tenant.entity.service_offer;

import eu.gaiax.wizard.dao.master.entity.LabelLevelQuestionMaster;
import eu.gaiax.wizard.dao.tenant.entity.SuperEntity;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "label_level_answer")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LabelLevelAnswer extends SuperEntity {
    @Column(name = "participant_id", insertable = false, updatable = false)
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", referencedColumnName = "id", nullable = false)
    private Participant participant;

    @Column(name = "service_offer_id", insertable = false, updatable = false)
    private UUID serviceOfferId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offer_id", referencedColumnName = "id", nullable = false)
    private ServiceOffer serviceOffer;

    @Column(name = "question_id", insertable = false, updatable = false)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", referencedColumnName = "id", nullable = false)
    private LabelLevelQuestionMaster question;

    private boolean answer;
}
