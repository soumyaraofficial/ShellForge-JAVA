import java.io.PrintStream;
import java.util.List;

public class CompleteBuiltin {

    // =============================================================
    // EXECUTE
    //
    // Registers programmable completions, e.g.:
    //
    //   complete -C /path/to/completer_script git
    //
    // For now this is a no-op: the builtin just needs to be
    // recognized (by `type`) and not fall through to
    // "command not found" when invoked directly. The actual
    // "-C script" registration and invocation logic - and its
    // hookup into FileNameCompleter's TAB handling - lands here
    // in a later stage.
    // =============================================================

    public void execute(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput) {

        // Intentionally empty for now.
    }
}