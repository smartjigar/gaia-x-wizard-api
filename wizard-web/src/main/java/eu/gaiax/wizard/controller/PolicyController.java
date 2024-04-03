package eu.gaiax.wizard.controller;

import eu.gaiax.wizard.api.model.CommonResponse;
import eu.gaiax.wizard.api.model.service_offer.ODRLPolicyRequest;
import eu.gaiax.wizard.api.model.service_offer.PolicyEvaluationRequest;
import eu.gaiax.wizard.core.service.service_offer.PolicyService;
import eu.gaiax.wizard.utils.WizardRestConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static eu.gaiax.wizard.api.utils.StringPool.SPATIAL;
import static eu.gaiax.wizard.utils.WizardRestConstant.PUBLIC_POLICY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@Tag(name = "Policy")
public class PolicyController extends BaseController {

    private final PolicyService policyService;

    @Tag(name = "Policy")
    @Operation(summary = "Create Policy")
    @PostMapping(path = PUBLIC_POLICY, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public CommonResponse<Map<String, Object>> createODRLPolicy(@Valid @RequestBody ODRLPolicyRequest odrlPolicyRequest) {
        return CommonResponse.of(policyService.createServiceOfferPolicy(odrlPolicyRequest, null));
    }

    @Tag(name = "Policy")
    @Operation(summary = "Policy evaluator for catalogue")
    @PostMapping(path = WizardRestConstant.POLICY_EVALUATE, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> evaluatePolicy(
            @Valid @RequestBody PolicyEvaluationRequest policyEvaluationRequest) {
        return ResponseEntity.ok(Map.of(SPATIAL, policyService.evaluatePolicy(policyEvaluationRequest)));
    }
}
