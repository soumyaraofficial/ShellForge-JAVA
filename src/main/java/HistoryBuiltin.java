import java.io.PrintStream;
import java.util.List;

public class HistoryBuiltin {

    // =============================================================
    // EXECUTE
    //
    // Supports:
    //   history        -> print every recorded entry
    //   history <n>    -> print only the last n entries
    //   history -r <path> -> read file into in-memory history
    //   history -w <path> -> overwrite file with full history
    //   history -a <path> -> append only unpersisted entries
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

        switch (arg) {

            case "-r":

                if (args.size() < 3) {

                    errorOutput.println(
                            "history: -r: option requires an argument"
                    );

                    return;
                }

                HistoryManager.readFromFile(args.get(2), errorOutput);
                return;

            case "-w":

                if (args.size() < 3) {

                    errorOutput.println(
                            "history: -w: option requires an argument"
                    );

                    return;
                }

                HistoryManager.writeToFile(args.get(2), errorOutput);
                return;

            case "-a":

                if (args.size() < 3) {

                    errorOutput.println(
                            "history: -a: option requires an argument"
                    );

                    return;
                }

                HistoryManager.appendNewToFile(args.get(2), errorOutput);
                return;

            default:
                break;
        }

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