import java.util.Map;
import java.util.TreeMap;

// =================================================================
// SHELL VARIABLE STORE
//
// Backs the `declare` builtin and `$VAR` / `${VAR}` parameter
// expansion. Static so it survives across CommandExecutor
// invocations and persists for the lifetime of the shell process -
// the same pattern already used by CompletionRegistry and
// JobManager in this project.
// =================================================================

public final class ShellVariables {

    private ShellVariables() {
    }

    private static final Map<String, String> VARIABLES =
            new TreeMap<>();

    // =============================================================
    // IDENTIFIER VALIDATION
    //
    // Must start with a letter or underscore, then contain only
    // letters, digits, and underscores.
    // =============================================================

    public static boolean isValidIdentifier(String name) {

        if (name == null || name.isEmpty()) {
            return false;
        }

        char first = name.charAt(0);

        if (!(Character.isLetter(first) || first == '_')) {
            return false;
        }

        for (int i = 1; i < name.length(); i++) {

            char c = name.charAt(i);

            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }

        return true;
    }

    // =============================================================
    // SET / GET / HAS
    // =============================================================

    public static void set(String name, String value) {
        VARIABLES.put(name, value);
    }

    public static String get(String name) {

        if (name == null) {
            return null;
        }

        return VARIABLES.get(name);
    }

    public static boolean has(String name) {

        if (name == null) {
            return false;
        }

        return VARIABLES.containsKey(name);
    }

    // =============================================================
    // ALL (used by `declare -p` with no name argument)
    //
    // Returns a fresh copy so callers can't mutate the shared store.
    // =============================================================

    public static Map<String, String> all() {
        return new TreeMap<>(VARIABLES);
    }
}