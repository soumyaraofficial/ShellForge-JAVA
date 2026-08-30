import java.io.PrintStream;
import java.util.List;

public class HistoryBuiltin {

    // =============================================================
    // EXECUTE
    //
    // Supports:
    //   history        -> print every recorded entry
    //   history <n>    -> print only the last n entries
    //
    // Delegates entirely to HistoryManager, which owns the actual
    // history data (backed by JLine's own History object), the
    // same way JobsBuiltin delegates to JobManager.
    // =============================================================

    public void execute(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput) {

        if (args.size() < 2) {

            HistoryManager.printAll(output);
            return;
        }

        String arg = args.get(1);

        int n;

        try {

            n = Integer.parseInt(arg);

        } catch (NumberFormatException e) {

            errorOutput.println(
                    "history: "
                            + arg
                            + ": numeric argument required"
            );

            return;
        }

        HistoryManager.printLast(n, output);
    }
}