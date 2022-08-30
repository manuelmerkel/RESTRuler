package rest.studentproject.rule.constants;

public class ImprovementSuggestion {

    // Add constant improvement suggestions here
    public static final String UNDERSCORE = "Use hyphens (-) instead of underscores (_)";
    public static final String LOWERCASE = "Change uppercase letters to lowercase letters";
    public static final String SEPARATOR = "remove any '#' and '?' from the path";
    public static final String SEPARATOR_UNKNOWN = "Please check validity of path";
    public static final String HYPHEN = "Use hyphens to improve the readability of the segments";

    private ImprovementSuggestion() {
        throw new IllegalStateException("Utility class");
    }
}
