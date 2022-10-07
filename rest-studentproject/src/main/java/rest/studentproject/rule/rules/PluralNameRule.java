package rest.studentproject.rule.rules;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rest.studentproject.rule.IRestRule;
import rest.studentproject.rule.Violation;
import rest.studentproject.rule.constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static rest.studentproject.analyzer.RestAnalyzer.locMapper;
import static rest.studentproject.oxford.dictionary.api.JsonOxfordDictionary.checkIfWordInJson;
import static rest.studentproject.oxford.dictionary.api.OxfordConstants.PLURAL;
import static rest.studentproject.oxford.dictionary.api.OxfordConstants.SINGULAR;
import static rest.studentproject.oxford.dictionary.api.OxfordDictionariesApi.checkWordUsingOxfordDictionariesAPI;
import static rest.studentproject.rule.Utility.getControlPathSegmentForRule;
import static rest.studentproject.rule.Utility.getSwitchPathSegment;

public class PluralNameRule implements IRestRule {

    public static final String WITH_PATH_SEGMENT = " With pathSegment: ";
    private static final String TITLE = "A plural noun should be used for collection or store names";
    private static final RuleCategory RULE_CATEGORY = RuleCategory.URIS;
    private static final RuleSeverity RULE_SEVERITY = RuleSeverity.ERROR;
    private static final List<RuleType> RULE_TYPE = List.of(RuleType.STATIC);
    private static final List<RuleSoftwareQualityAttribute> RULE_SOFTWARE_QUALITY_ATTRIBUTE_LIST =
            List.of(RuleSoftwareQualityAttribute.USABILITY, RuleSoftwareQualityAttribute.MAINTAINABILITY);
    private boolean isActive;

    public PluralNameRule(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public RuleCategory getCategory() {
        return RULE_CATEGORY;
    }

    @Override
    public RuleSeverity getSeverityType() {
        return RULE_SEVERITY;
    }

    @Override
    public List<RuleType> getRuleType() {
        return RULE_TYPE;
    }

    @Override
    public List<RuleSoftwareQualityAttribute> getRuleSoftwareQualityAttribute() {
        return RULE_SOFTWARE_QUALITY_ATTRIBUTE_LIST;
    }

    @Override
    public boolean getIsActive() {
        return this.isActive;
    }

    @Override
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public List<Violation> checkViolation(OpenAPI openAPI) {
        List<Violation> violations = new ArrayList<>();

        // Get the paths from the OpenAPI object
        Set<String> paths = openAPI.getPaths().keySet();

        if (paths.isEmpty()) return violations;
        // Loop through the paths
        return getLstViolations(violations, paths);
    }

    private List<Violation> getLstViolations(List<Violation> violations, Set<String> paths) {
        for (String path : paths) {
            if (path.trim().equals("")) continue;
            // Get the path without the curly braces
            String[] pathSegments = path.split("/");
            // Extract path segments based on / char and check if there are violations
            Violation violation = getLstViolationsFromPathSegments(path, pathSegments);
            if (violation != null) violations.add(violation);


        }
        return violations;
    }

    private Violation getLstViolationsFromPathSegments(String path, String[] pathSegments) {
        String switchPathSegment = "";
        String firstPathSegment = "";
        boolean skipFirstSwitchPathSegmentAssign = true;
        List<String> listPathSegments = new ArrayList<>(List.of(pathSegments));
        listPathSegments.removeAll(Arrays.asList("", null));
        // Check if a path is starting with a plural or singular word.
        if (!listPathSegments.isEmpty()) firstPathSegment = listPathSegments.get(0).trim().toLowerCase();
        // Set the switch based on the firstPathSegment. We need to see if a path has the form
        // singular/plural/singular.. or plural/singular/plural.. based on the firstPathSegment
        switchPathSegment = getSwitchPathSegment(pathSegments, switchPathSegment, firstPathSegment);

        for (String pathSegment : listPathSegments) {
            if (pathSegment.isEmpty()) continue;
            // If a pathSegment contains a curly brace, it is a parameter, and we don't need to check it. But we know
            // that such a pathSegment is automatically singular.
            // In this case is important to see if the previous switchPathSegment is plural or singular. If was
            // singular,and we have a pathSegment with curly braces, then we have a violation because
            // singular/singular path.
            if (pathSegment.contains("{") && switchPathSegment.equals(SINGULAR)) {
                return new Violation(this, locMapper.getLOCOfPath(path), ImprovementSuggestion.SINGULAR_DOCUMENT_NAME
                        , path, ErrorMessage.PLURAL_NAME + WITH_PATH_SEGMENT + pathSegment);
            } else if (pathSegment.contains("{") && switchPathSegment.equals(PLURAL)) {
                // Switch to plural because the curly brace pathSegment is singular and the previous pathSegment was
                // plural.
                switchPathSegment = SINGULAR;
                continue;
            }
            // Check if the jsonDictionary already contained the pathSegment. The return value is a tuple, with a
            // boolean (true if the word is present) and a string (the word in singular or plural form)
            ImmutablePair<Boolean, String> isPathSegmentInJson = checkIfWordInJson(pathSegment);
            // If the word is contained and the form of the word doesn't match the current switchPathSegment then we
            // have a violation
            if (Boolean.TRUE.equals(isPathSegmentInJson.getLeft()) && switchPathSegment.equals(isPathSegmentInJson.getRight()) && switchPathSegment.equals(SINGULAR) && !listPathSegments.get(0).equals(pathSegment)) {
                return new Violation(this, locMapper.getLOCOfPath(path), ImprovementSuggestion.PLURAL_NAME, path,
                        ErrorMessage.PLURAL_NAME + WITH_PATH_SEGMENT + pathSegment);
            } else if (Boolean.TRUE.equals(isPathSegmentInJson.getLeft())) {
                switchPathSegment = skipFirstSwitchPathSegmentAssign ? switchPathSegment :
                        getControlPathSegmentForRule(switchPathSegment.equals(PLURAL));
                skipFirstSwitchPathSegmentAssign = false;
                continue;
            }

            // If the word is not contained in the jsonDictionary, we need to check if the word is present in the
            // OxfordDictionaryAPI
            // We get a true if the word is singular otherwise a false if it is plural
            boolean isSingular = checkWordUsingOxfordDictionariesAPI(pathSegment.trim().toLowerCase());
            // If the word is singular but the current switchPathSegment is plural, then we have a violation.
            if (isSingular && switchPathSegment.equals(SINGULAR) && !listPathSegments.get(0).equals(pathSegment)) {
                return new Violation(this, locMapper.getLOCOfPath(path), ImprovementSuggestion.PLURAL_NAME, path,
                        ErrorMessage.PLURAL_NAME + WITH_PATH_SEGMENT + pathSegment);
            }

            switchPathSegment = skipFirstSwitchPathSegmentAssign ? switchPathSegment :
                    getControlPathSegmentForRule(switchPathSegment.equals(PLURAL));
            skipFirstSwitchPathSegmentAssign = false;
        }
        return null;
    }

}
