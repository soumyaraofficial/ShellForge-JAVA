import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Quoting {

    public static List<String> parseCommand(String command) {

        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean argumentStarted = false;

        char[] chars = command.toCharArray();

        for (int i = 0; i < chars.length; i++) {

            char c = chars[i];

            // ==========================================
            // INSIDE DOUBLE QUOTES
            // ==========================================
            if (inDoubleQuote) {

                if (c == '\\') {

                    if (i + 1 < chars.length) {

                        char next = chars[i + 1];

                        /*
                         * Inside double quotes:
                         *
                         * \" -> "
                         * \\ -> \
                         *
                         * Other backslashes remain literal.
                         */
                        if (next == '"' || next == '\\') {
                            current.append(next);
                            argumentStarted = true;
                            i++;
                            continue;
                        }
                    }

                    // Backslash before any other character
                    // remains a literal backslash.
                    current.append('\\');
                    argumentStarted = true;

                } else if (c == '"') {

                    // End of double-quoted section
                    inDoubleQuote = false;
                    argumentStarted = true;

                } else {

                    current.append(c);
                    argumentStarted = true;
                }

            }

            // ==========================================
            // INSIDE SINGLE QUOTES
            // ==========================================
            else if (inSingleQuote) {

                if (c == '\'') {

                    // End of single-quoted section
                    inSingleQuote = false;
                    argumentStarted = true;

                } else {

                    // Everything is literal inside single quotes
                    current.append(c);
                    argumentStarted = true;
                }

            }

            // ==========================================
            // OUTSIDE QUOTES
            // ==========================================
            else {

                if (c == '\\') {

                    /*
                     * Outside quotes:
                     *
                     * \x -> x
                     */
                    if (i + 1 < chars.length) {
                        current.append(chars[++i]);
                        argumentStarted = true;
                    }

                } else if (c == '\'') {

                    // Start single-quoted section
                    inSingleQuote = true;
                    argumentStarted = true;

                } else if (c == '"') {

                    // Start double-quoted section
                    inDoubleQuote = true;
                    argumentStarted = true;

                } else if (Character.isWhitespace(c)) {

                    /*
                     * Space separates arguments,
                     * but only if we have actually started
                     * an argument.
                     */
                    if (argumentStarted) {

                        args.add(current.toString());

                        current.setLength(0);
                        argumentStarted = false;
                    }

                } else {

                    current.append(c);
                    argumentStarted = true;
                }
            }
        }

        // Add final argument
        if (argumentStarted) {
            args.add(current.toString());
        }

        return args;
    }


    public void executeEcho(List<String> commandSplit, PrintStream output) {

        StringBuilder string = new StringBuilder();

        for (int i = 1; i < commandSplit.size(); i++) {

            if (i > 1) {
                string.append(" ");
            }

            string.append(commandSplit.get(i));
        }

        output.println(string);
    }
}