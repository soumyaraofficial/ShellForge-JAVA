import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExternalCompleter {

    private ExternalCompleter() {
    }

    // =============================================================
    // GET CANDIDATES
    //
    // Runs the registered completer script as a child process,
    // waits for it to finish, and returns its stdout lines as
    // sorted candidates. Any failure (bad path, non-executable,
    // exception, non-zero exit) is treated as "no candidates" -
    // it must never crash the shell or print debug output.
    // =============================================================

    public static List<String> getCandidates(
            String scriptPath,
            String buffer,
            int cursor) {

        List<String> candidates = new ArrayList<>();

        String command = extractCommand(buffer);
        String currentWord = extractCurrentWord(buffer);
        String previousWord = extractPreviousWord(buffer);

        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            scriptPath,
                            command,
                            currentWord,
                            previousWord
                    );

            processBuilder.environment().put(
                    "COMP_LINE",
                    buffer
            );

            processBuilder.environment().put(
                    "COMP_POINT",
                    String.valueOf(byteIndex(buffer, cursor))
            );

            Process process =
                    processBuilder.start();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream(),
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    candidates.add(line);
                }
            }

            process.waitFor();

        } catch (Exception e) {

            return new ArrayList<>();
        }

        Collections.sort(candidates);

        return candidates;
    }

    // =============================================================
    // argv[1] - command name being completed
    // =============================================================

    private static String extractCommand(String buffer) {

        String trimmed = buffer.stripLeading();

        int spaceIndex = trimmed.indexOf(' ');

        return spaceIndex == -1
                ? trimmed
                : trimmed.substring(0, spaceIndex);
    }

    // =============================================================
    // argv[2] - word currently being completed
    // =============================================================

    private static String extractCurrentWord(String buffer) {

        int lastSpace = buffer.lastIndexOf(' ');

        return buffer.substring(lastSpace + 1);
    }

    // =============================================================
    // argv[3] - the token immediately preceding the current word
    // (this includes the command name itself when the current
    // word is the first argument, e.g. "git pu" -> prev = "git").
    // "" only when there is truly nothing before the current word.
    // =============================================================

    private static String extractPreviousWord(String buffer) {

        String[] words = tokenize(buffer);

        if (words.length < 2) {
            return "";
        }

        return words[words.length - 2];
    }

    private static String[] tokenize(String buffer) {

        List<String> tokens = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (int i = 0; i < buffer.length(); i++) {

            char c = buffer.charAt(i);

            if (c == ' ') {

                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

            } else {

                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        } else {
            tokens.add("");
        }

        return tokens.toArray(new String[0]);
    }

    // =============================================================
    // COMP_POINT - zero-based BYTE index of the cursor, not a
    // char index (matters for multibyte/UTF-8 input).
    // =============================================================

    private static int byteIndex(String buffer, int cursor) {

        int charIndex =
                Math.min(Math.max(cursor, 0), buffer.length());

        String upToCursor =
                buffer.substring(0, charIndex);

        return upToCursor.getBytes(StandardCharsets.UTF_8).length;
    }
}