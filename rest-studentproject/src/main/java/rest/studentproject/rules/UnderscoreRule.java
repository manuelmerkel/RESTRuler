package rest.studentproject.rules;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import rest.studentproject.analyzer.LOCMapper;
import rest.studentproject.analyzer.RestAnalyzer;
import rest.studentproject.rules.constants.RuleCategory;
import rest.studentproject.rules.constants.RuleSeverity;
import rest.studentproject.rules.constants.RuleSoftwareQualityAttribute;
import rest.studentproject.rules.constants.RuleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the rule: Underscores (_) should not be used in URI.
 */
public class UnderscoreRule implements IRestRule {
    private final List<Violation> violationList = new ArrayList<>();
    private boolean isActive;
    private LOCMapper locMapper;
    private String title = "Underscores (_) should not be used in URI";
    private RuleCategory category = RuleCategory.URIS;
    private RuleSeverity severity = RuleSeverity.ERROR;
    private RuleType type = RuleType.STATIC;
    private List<RuleSoftwareQualityAttribute> softwareQualityAttributes = Arrays.asList(RuleSoftwareQualityAttribute.MAINTAINABILITY);

    public UnderscoreRule(boolean isActive) {
        setIsActive(isActive);
        this.locMapper = RestAnalyzer.locMapper;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public RuleCategory getCategory() {
        return this.category;
    } 

    @Override
    public RuleSeverity getSeverityType() {
        return this.severity;
    }

    @Override
    public RuleType getRuleType() {
        return this.type;
    }

    @Override
    public List<RuleSoftwareQualityAttribute> getRuleSoftwareQualityAttribute() {
        return this.softwareQualityAttributes;
    }

    @Override
    public boolean getIsActive() {
        return this.isActive;
    }

    @Override
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Checks if there is a violation against the underscore rule. All paths and base URLs are checked.
     *
     * @param openAPI the definition that will be checked against the rule.
     * @return the list of violations.
     */
    public List<Violation> checkViolation(OpenAPI openAPI) {
        Set<String> paths = openAPI.getPaths().keySet();
        List<Server> servers = openAPI.getServers();


        for (String path : paths) {
            if (path.trim().isEmpty()) continue;
            checkUnderscore(path);
        }

        for (Server server : servers) {
            if (server.getUrl().trim().isEmpty()) continue;
            checkUnderscore(server.getUrl());
        }
        return this.violationList.isEmpty() ? null : this.violationList;
    }

    /**
     * Checks if the given path contains an underscore. If there is a parameter within the path, it will be deleted.
     *
     * @param path the path to check if it contains an underscore.
     */
    private void checkUnderscore(String path) {
        String pathWithoutVariable = path.replaceAll("\\{" + ".*" + "\\}", "");
        if (!pathWithoutVariable.contains("_")) return;
        this.violationList.add(new Violation(this.locMapper.getLOCOfPath(path), "", path, ""));

    }
}
