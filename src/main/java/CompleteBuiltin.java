import java.io.PrintStream;
import java.util.List;

public class CompleteBuiltin {

    // =============================================================
    // EXECUTE
    //
    // Supports:
    //   complete -p <command>
    //   complete -C <script_path> <command>
    //   complete -r <command>
    // =============================================================

    public void execute(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput) {

        if (args.size() < 2) {
            return;
        }

        String flag = args.get(1);

        switch (flag) {

            case "-p":
                executeShow(args, output);
                return;

            case "-C":
                executeRegister(args);
                return;

            case "-r":
                executeRemove(args);
                return;

            default:
                return;
        }
    }

    // =============================================================
    // -p : print stored registration, reconstructed from state
    // =============================================================

    private void executeShow(
            List<String> args,
            PrintStream output) {

        if (args.size() < 3) {
            return;
        }

        String command = args.get(2);

        String scriptPath =
                CompletionRegistry.get(command);

        if (scriptPath == null) {

            output.println(
                    "complete: "
                            + command
                            + ": no completion specification"
            );

        } else {

            output.println(
                    "complete -C '"
                            + scriptPath
                            + "' "
                            + command
            );
        }
    }

    // =============================================================
    // -C : register (or replace) a completer for a command
    // =============================================================

    private void executeRegister(List<String> args) {

        if (args.size() < 4) {
            return;
        }

        String scriptPath = args.get(2);
        String command = args.get(3);

        CompletionRegistry.register(command, scriptPath);
    }

    // =============================================================
    // -r : remove a registration (no-op if it never existed)
    // =============================================================

    private void executeRemove(List<String> args) {

        if (args.size() < 3) {
            return;
        }

        String command = args.get(2);

        CompletionRegistry.remove(command);
    }
}