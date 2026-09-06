import java.io.PrintStream;
import java.util.List;
import java.util.Map;

// =================================================================
// DECLARE BUILTIN
//
// Supports:
//   declare NAME=VALUE   -> store/update a shell variable
//   declare NAME         -> declare a variable with an empty value
//                           (only if it doesn't already exist)
//   declare -p NAME      -> print `declare -- NAME="VALUE"`, or
//                           `declare: NAME: not found` if unset
//   declare -p           -> print every stored variable
//   declare              -> same as `declare -p` with no name
//
// Delegates all storage to ShellVariables, the same way other
// builtins in this project (HistoryBuiltin, JobsBuiltin,
// CompleteBuiltin) delegate to their own static manager classes.
// =================================================================

public class DeclareBuiltin {

    public void execute(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput) {

        if (args.size() < 2) {

            printAll(output);
            return;
        }

        if (args.get(1).equals("-p")) {

            executeShow(args, output, errorOutput);
            return;
        }

        for (int i = 1; i < args.size(); i++) {
            executeAssignment(args.get(i), errorOutput);
        }
    }

    // =============================================================
    // declare -p [NAME ...]
    // =============================================================

    private void executeShow(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput) {

        if (args.size() < 3) {

            printAll(output);
            return;
        }

        for (int i = 2; i < args.size(); i++) {

            String name = args.get(i);

            if (!ShellVariables.has(name)) {

                errorOutput.println(
                        "declare: " + name + ": not found"
                );

                continue;
            }

            output.println(
                    formatDeclare(name, ShellVariables.get(name))
            );
        }
    }

    private void printAll(PrintStream output) {

        for (Map.Entry<String, String> entry :
                ShellVariables.all().entrySet()) {

            output.println(
                    formatDeclare(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }
    }

    // =============================================================
    // declare NAME=VALUE / declare NAME
    // =============================================================

    private void executeAssignment(
            String token,
            PrintStream errorOutput) {

        int equalsIndex = token.indexOf('=');

        String name =
                equalsIndex == -1
                        ? token
                        : token.substring(0, equalsIndex);

        if (!ShellVariables.isValidIdentifier(name)) {

            errorOutput.println(
                    "declare: `" + token + "': not a valid identifier"
            );

            return;
        }

        if (equalsIndex == -1) {

            if (!ShellVariables.has(name)) {
                ShellVariables.set(name, "");
            }

            return;
        }

        String value = token.substring(equalsIndex + 1);

        ShellVariables.set(name, value);
    }

    private String formatDeclare(String name, String value) {

        return "declare -- " + name + "=\"" + value + "\"";
    }
}