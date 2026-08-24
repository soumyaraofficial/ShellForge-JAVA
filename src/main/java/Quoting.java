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

            // =====================================================
            // INSIDE DOUBLE QUOTES
            // =====================================================

            if (inDoubleQuote) {

                // Backslash inside double quotes
                if (c == '\\') {

                    if (i + 1 < chars.length) {

                        char next = chars[i + 1];

                        /*
                         * Inside double quotes:
                         *
                         * \" -> "
                         * \\ -> \
                         *
                         * For any other character, the backslash
                         * remains literal.
                         */
                        if (next == '"' || next == '\\') {

                            current.append(next);

                            i++;

                        } else {

                            current.append('\\');
                        }

                    } else {

                        // Backslash at the end
                        current.append('\\');
                    }

                    argumentStarted = true;

                }

                // Closing double quote
                else if (c == '"') {

                    inDoubleQuote = false;
                    argumentStarted = true;

                }

                // Normal character
                else {

                    current.append(c);
                    argumentStarted = true;
                }
            }

            // =====================================================
            // INSIDE SINGLE QUOTES
            // =====================================================

            else if (inSingleQuote) {

                /*
                 * Everything is literal inside single quotes.
                 *
                 * Example:
                 *
                 * 'hello\world'
                 *
                 * becomes:
                 *
                 * hello\world
                 */

                if (c == '\'') {

                    // Closing single quote
                    inSingleQuote = false;
                    argumentStarted = true;

                } else {

                    current.append(c);
                    argumentStarted = true;
                }
            }

            // =====================================================
            // OUTSIDE QUOTES
            // =====================================================

            else {

                // -------------------------------------------------
                // BACKSLASH OUTSIDE QUOTES
                // -------------------------------------------------

                if (c == '\\') {

                    /*
                     * Outside quotes:
                     *
                     * \x -> x
                     *
                     * Examples:
                     *
                     * hello\ world
                     * -> hello world
                     *
                     * hello\"
                     * -> hello"
                     *
                     * hello\\
                     * -> hello\
                     */

                    if (i + 1 < chars.length) {

                        current.append(chars[i + 1]);

                        i++;

                        argumentStarted = true;

                    } else {

                        // Literal trailing backslash
                        current.append('\\');
                        argumentStarted = true;
                    }
                }

                // -------------------------------------------------
                // START SINGLE QUOTE
                // -------------------------------------------------

                else if (c == '\'') {

                    inSingleQuote = true;
                    argumentStarted = true;
                }

                // -------------------------------------------------
                // START DOUBLE QUOTE
                // -------------------------------------------------

                else if (c == '"') {

                    inDoubleQuote = true;
                    argumentStarted = true;
                }

                // -------------------------------------------------
                // WHITESPACE
                // -------------------------------------------------

                else if (Character.isWhitespace(c)) {

                    /*
                     * Whitespace separates arguments only when
                     * an argument has already started.
                     */

                    if (argumentStarted) {

                        args.add(current.toString());

                        current.setLength(0);

                        argumentStarted = false;
                    }
                }

                // -------------------------------------------------
                // NORMAL CHARACTER
                // -------------------------------------------------

                else {

                    current.append(c);
                    argumentStarted = true;
                }
            }
        }

        // =========================================================
        // ADD FINAL ARGUMENT
        // =========================================================

        if (argumentStarted) {

            args.add(current.toString());
        }

        return args;
    }

    // =============================================================
    // ECHO
    // =============================================================

    public void executeEcho(
            List<String> commandSplit,
            PrintStream output) {

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