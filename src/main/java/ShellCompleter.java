import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class ShellCompleter implements Completer {

    private final Completer commandCompleter;
    private final Completer fileNameCompleter;

    public ShellCompleter(
            Completer commandCompleter,
            Completer fileNameCompleter) {

        this.commandCompleter = commandCompleter;
        this.fileNameCompleter = fileNameCompleter;
    }

    @Override
    public void complete(
            LineReader reader,
            ParsedLine line,
            List<Candidate> candidates) {

        /*
         * If there is no whitespace before the cursor,
         * we are completing the command itself.
         *
         * Example:
         *
         * xyz_
         *
         * Use command completion.
         */
        if (isCompletingCommand(line)) {

            commandCompleter.complete(
                    reader,
                    line,
                    candidates
            );

            return;
        }

        /*
         * Once the user has typed a space, we are
         * completing an argument.
         *
         * Example:
         *
         * wc banana
         *
         * Use filename completion.
         */
        fileNameCompleter.complete(
                reader,
                line,
                candidates
        );
    }

    private boolean isCompletingCommand(
            ParsedLine line) {

        String input = line.line();

        /*
         * Look at everything before the cursor.
         *
         * If there is no whitespace, the user is
         * still typing the command name.
         */
        int cursor = line.cursor();

        for (int i = 0; i < cursor; i++) {

            if (Character.isWhitespace(
                    input.charAt(i))) {

                return false;
            }
        }

        return true;
    }
}