import org.jline.reader.History;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class HistoryManager {

    private HistoryManager() {
    }

    // =============================================================
    // Backing store is JLine's own History object - the exact same
    // one the LineReader uses for UP/DOWN arrow recall - set once
    // from Shell right after the reader is built. This keeps a
    // single source of truth instead of maintaining a second,
    // parallel command list (mirrors how JobManager backs `jobs`).
    // =============================================================

    private static History history;

    public static void setHistory(History jlineHistory) {
        history = jlineHistory;
    }

    // =============================================================
    // `history` BUILTIN OUTPUT
    //
    // Prints every entry currently held by JLine's history, using
    // the same "%5d  %s" numbering bash uses, numbered from 1.
    // =============================================================

    public static void printAll(PrintStream output) {

        for (History.Entry entry : snapshot()) {
            printEntry(output, entry);
        }
    }

    // =============================================================
    // `history <n>` BUILTIN OUTPUT
    //
    // Prints only the last n entries (this includes the current
    // `history <n>` invocation itself, since by the time this runs
    // JLine has already recorded it). n <= 0 prints nothing; n
    // larger than the history size prints everything available.
    // =============================================================

    public static void printLast(int n, PrintStream output) {

        if (n <= 0) {
            return;
        }

        List<History.Entry> entries = snapshot();

        int start = Math.max(0, entries.size() - n);

        for (int i = start; i < entries.size(); i++) {
            printEntry(output, entries.get(i));
        }
    }

    private static List<History.Entry> snapshot() {

        List<History.Entry> entries = new ArrayList<>();

        if (history == null) {
            return entries;
        }

        for (History.Entry entry : history) {
            entries.add(entry);
        }

        return entries;
    }

    private static void printEntry(
            PrintStream output,
            History.Entry entry) {

        output.println(
                String.format(
                        "%5d  %s",
                        entry.index() + 1,
                        entry.line()
                )
        );
    }
}