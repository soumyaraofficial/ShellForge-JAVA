import java.util.Set;
import java.util.TreeSet;

public final class Builtins {

    private Builtins() {
    }

    private static final Set<String> NAMES =
            new TreeSet<>();

    static {
        NAMES.add("echo");
        NAMES.add("exit");
        NAMES.add("type");
        NAMES.add("pwd");
        NAMES.add("cd");
        NAMES.add("complete");
    }

    // =============================================================
    // NAMES
    //
    // Returns a fresh copy each call so callers (e.g. Shell's
    // command set, which also gets PATH executables merged in)
    // can't accidentally mutate the shared canonical list.
    // =============================================================

    public static Set<String> names() {
        return new TreeSet<>(NAMES);
    }

    // =============================================================
    // IS BUILTIN
    //
    // Case-insensitive, matching the comparison CommandExecutor
    // used previously.
    // =============================================================

    public static boolean isBuiltin(String command) {

        if (command == null) {
            return false;
        }

        for (String name : NAMES) {

            if (name.equalsIgnoreCase(command)) {
                return true;
            }
        }

        return false;
    }
}