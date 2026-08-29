import java.util.Map;
import java.util.TreeMap;

public final class CompletionRegistry {

    private CompletionRegistry() {
    }

    // =============================================================
    // command -> completer script path
    //
    // Static so it survives the fact that CommandExecutor creates
    // a fresh CompleteBuiltin instance on every invocation.
    // =============================================================

    private static final Map<String, String> COMPLETERS =
            new TreeMap<>();

    public static void register(String command, String scriptPath) {

        if (command == null || scriptPath == null) {
            return;
        }

        COMPLETERS.put(command, scriptPath);
    }

    public static void remove(String command) {

        if (command == null) {
            return;
        }

        COMPLETERS.remove(command);
    }

    public static String get(String command) {

        if (command == null) {
            return null;
        }

        return COMPLETERS.get(command);
    }
}