package rest.studentproject.rule.rules;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import rest.studentproject.rule.IRestRule;
import rest.studentproject.rule.Violation;
import rest.studentproject.rule.constants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rest.studentproject.analyzer.RestAnalyzer.locMapper;
import static rest.studentproject.rule.Utility.splitContiguousWords;

public class VerbPhraseRule implements IRestRule {

    private static final String TITLE = "A verb or verb phrase should be used for controller names";
    private static final RuleCategory RULE_CATEGORY = RuleCategory.URIS;
    private static final RuleSeverity RULE_SEVERITY = RuleSeverity.ERROR;
    private static final RuleType RULE_TYPE = RuleType.STATIC;
    private static final List<RuleSoftwareQualityAttribute> RULE_SOFTWARE_QUALITY_ATTRIBUTE_LIST = List.of(RuleSoftwareQualityAttribute.USABILITY, RuleSoftwareQualityAttribute.MAINTAINABILITY);
    public static final String MODELS_EN_POS_MAXENT_BIN = "models/en-pos-maxent.bin";
    private boolean isActive;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public VerbPhraseRule(boolean isActive) {
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
    public RuleType getRuleType() {
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
        Paths openApiPaths = openAPI.getPaths();
        // Get the paths from the OpenAPI object
        Set<String> paths = openAPI.getPaths().keySet();

        if (paths.isEmpty()) return violations;
        // Loop through the paths
        return getLstViolations(violations, openApiPaths);
    }

    private List<Violation> getLstViolations(List<Violation> violations, Paths paths) {
        paths.forEach((path, pathItem) -> {
            if (!path.trim().equals("")) {
                // Check if the path is of type get or post
                Operation getOperation = pathItem.getGet();
                Operation postOperation = pathItem.getPost();
                // Get the path without the curly braces
                String pathWithoutVariables = path.replaceAll("\\{" + ".*" + "\\}/", "");
                String[] pathSegments = pathWithoutVariables.split("/");
                // Extract path segments based on / char and check if there are violations
                Violation violation = getLstViolationsFromPathSegments(path, pathSegments, getOperation, postOperation);
                if (violation != null) violations.add(violation);
                Operation operationGet = pathItem.getGet();
                Operation operationPost = pathItem.getPost();
            }
        });
        return violations;
    }

    private Violation getLstViolationsFromPathSegments(String path, String[] pathSegments, Operation getOperation, Operation postOperation) {
        // Get the last pathSegment which we need to analyze
        if(pathSegments.length < 1) return null;
        String lastPathSegment = pathSegments[pathSegments.length-1];
        try {
            // Get the words forming the pathSegment
            List<String> subStringFromPath = splitContiguousWords(lastPathSegment);
            List<String> pathWithoutParameterDictionaryMatching = Arrays.asList(subStringFromPath.get(0).split(" "));
            // Check if the first word is a verb
            if(pathWithoutParameterDictionaryMatching.get(0).equals("")) return null;

            String token = getTokenNLP(pathWithoutParameterDictionaryMatching.get(0));
            // If the first word is a verb but the request is not of type get or post then we have a violation.
            boolean isTokenVerb = token.equals("VBZ") || token.equals("VBP") || token.equals("VB");
            if (isTokenVerb && (getOperation == null && postOperation == null)) {
                return new Violation(this, locMapper.getLOCOfPath(path), ImprovementSuggestion.VERBPHRASE, path,
                        ErrorMessage.VERBPHRASE);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error on checking substring against a dictionary{e}", e);
        }
        return null;
    }



    public String getTokenNLP(String pathSegment){
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(pathSegment);
        try(InputStream modelIn = new FileInputStream(
                MODELS_EN_POS_MAXENT_BIN);) {
            POSModel posModel = new POSModel(modelIn);
            POSTaggerME posTagger = new POSTaggerME(posModel);
            String tags[] = posTagger.tag(tokens);
            return tags[0];
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}

