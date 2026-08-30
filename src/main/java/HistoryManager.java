import org.jline.reader.History;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    // =============================================================
    // PERSISTENCE BOUNDARY
    //
    // Number of entries (counted from the start of history) that
    // have already been written to a history file, either via
    // `history -w`, `history -a`, or the startup $HISTFILE load.
    // `history -a` and exit-time saving both append only the slice
    // AFTER this boundary, which is what prevents duplicate lines
    // when the two are combined in the same session.
    // =============================================================

    private static int lastPersistedIndex = 0;

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

    // =============================================================
    // `history -r <path>`
    //
    // Reads commands from the file and appends them to the current
    // in-memory (JLine) history, exactly like normally typed
    // commands would be added. Blank lines (including the trailing
    // one most history files end with) are skipped. Does NOT touch
    // the persistence boundary - these lines came from disk, they
    // were never "typed" and shouldn't be assumed already written
    // back to wherever a later `history -a`/exit save might target.
    // =============================================================

    public static void readFromFile(String path, PrintStream errorOutput) {

        if (history == null) {
            return;
        }

        List<String> lines = readLines(path, errorOutput);

        if (lines == null) {
            return;
        }

        for (String line : lines) {
            history.add(line);
        }
    }

    // =============================================================
    // `history -w <path>`
    //
    // Overwrites the file with the ENTIRE in-memory history,
    // command text only (no numbering), one per line, trailing
    // newline. After a successful write, everything currently in
    // memory is considered persisted.
    // =============================================================

    public static void writeToFile(String path, PrintStream errorOutput) {

        if (history == null) {
            return;
        }

        List<History.Entry> entries = snapshot();

        boolean ok = writeLines(path, entries, false, errorOutput);

        if (ok) {
            lastPersistedIndex = entries.size();
        }
    }

    // =============================================================
    // `history -a <path>`
    //
    // Appends only the entries added since the last successful
    // persistence operation (write, append, or startup load).
    // Calling this repeatedly with nothing new typed in between is
    // a safe no-op - it does not re-append or error.
    // =============================================================

    public static void appendNewToFile(String path, PrintStream errorOutput) {

        if (history == null) {
            return;
        }

        List<History.Entry> entries = snapshot();

        int start = Math.min(lastPersistedIndex, entries.size());

        List<History.Entry> newEntries =
                entries.subList(start, entries.size());

        if (newEntries.isEmpty()) {
            return;
        }

        boolean ok = writeLines(path, newEntries, true, errorOutput);

        if (ok) {
            lastPersistedIndex = entries.size();
        }
    }

    // =============================================================
    // STARTUP $HISTFILE LOAD
    //
    // Same file-reading behavior as `history -r`, but the loaded
    // entries are marked as already persisted (boundary advances
    // to cover them), since they came straight from the file that
    // exit-time saving will later append to. This is what stops
    // stage 6 from re-writing the pre-existing lines on exit.
    //
    // Silent no-op if the path is unset/blank or the file doesn't
    // exist - startup must never crash and must never create the
    // file just because it was missing.
    // =============================================================

    public static void loadStartupHistory(String path) {

        if (history == null || path == null || path.isBlank()) {
            return;
        }

        List<String> lines = readLines(path, null);

        if (lines == null) {
            return;
        }

        for (String line : lines) {
            history.add(line);
        }

        lastPersistedIndex = snapshot().size();
    }

    // =============================================================
    // EXIT-TIME PERSISTENCE
    //
    // Appends whatever hasn't been persisted yet to $HISTFILE. If
    // the file never existed (fresh session, no prior `history -a`
    // and nothing loaded at startup), the boundary is still 0, so
    // this naturally writes out the whole session's history - no
    // separate "write on exit" code path is needed. If `history -a`
    // was already called during the session, only the remaining
    // delta goes out, avoiding duplicates.
    // =============================================================

    public static void saveOnExit(String path) {

        if (path == null || path.isBlank()) {
            return;
        }

        appendNewToFile(path, System.err);
    }

    // =============================================================
    // INTERNAL HELPERS
    // =============================================================

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

    // =============================================================
    // READ LINES FROM A HISTORY FILE
    //
    // Skips blank lines (this both drops the trailing empty line
    // most history files end with, and matches how history files
    // are conventionally structured - one command per non-empty
    // line). Returns null on any failure; caller decides whether
    // to report it (errorOutput == null means "stay silent", used
    // by startup loading).
    // =============================================================

    private static List<String> readLines(
            String path,
            PrintStream errorOutput) {

        if (path == null || path.isBlank()) {

            if (errorOutput != null) {
                errorOutput.println("history: missing file operand");
            }

            return null;
        }

        File file = new File(path);

        if (!file.exists()) {

            if (errorOutput != null) {
                errorOutput.println(
                        "history: " + path + ": No such file or directory"
                );
            }

            return null;
        }

        if (file.isDirectory()) {

            if (errorOutput != null) {
                errorOutput.println(
                        "history: " + path + ": Is a directory"
                );
            }

            return null;
        }

        List<String> result = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty()) {
                    continue;
                }

                result.add(line);
            }

        } catch (IOException e) {

            if (errorOutput != null) {
                errorOutput.println(
                        "history: " + path + ": " + e.getMessage()
                );
            }

            return null;
        }

        return result;
    }

    // =============================================================
    // WRITE (OR APPEND) HISTORY ENTRIES TO A FILE
    //
    // Command text only, one per line, always ends with a trailing
    // newline. Creates parent directories if needed, same courtesy
    // CommandExecutor already extends to redirection targets.
    // =============================================================

    private static boolean writeLines(
            String path,
            List<History.Entry> entries,
            boolean append,
            PrintStream errorOutput) {

        if (path == null || path.isBlank()) {

            if (errorOutput != null) {
                errorOutput.println("history: missing file operand");
            }

            return false;
        }

        File file = new File(path);

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(file, append))) {

            for (History.Entry entry : entries) {
                writer.write(entry.line());
                writer.newLine();
            }

        } catch (IOException e) {

            if (errorOutput != null) {
                errorOutput.println(
                        "history: " + path + ": " + e.getMessage()
                );
            }

            return false;
        }

        return true;
    }
}