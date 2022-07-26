package rest.studentproject.rules;

import io.swagger.v3.oas.models.OpenAPI;
import rest.studentproject.rules.constants.RuleCategory;
import rest.studentproject.rules.constants.RuleSeverity;
import rest.studentproject.rules.constants.RuleSoftwareQualityAttribute;
import rest.studentproject.rules.constants.RuleType;

import java.util.List;

public interface IRestRule {

    String getTitle();

    RuleCategory getCategory();

    RuleSeverity getSeverityType();

    RuleType getRuleType();

    List<RuleSoftwareQualityAttribute> getRuleSoftwareQualityAttribute();

    boolean getIsActive();

    void setIsActive(boolean isActive);

    List<Violation> checkViolation(OpenAPI openAPI);
}
