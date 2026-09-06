import java.util.ArrayList;
import java.util.List;

// =================================================================
// PARAMETER EXPANSION
//
// Expands $NAME and ${NAME} references against ShellVariables,
// applied to already-tokenized arguments (i.e. after
// Quoting.parseCommand() has done its job), so quoting/escaping
// behavior is completely unaffected.
//
// Pipeline (per the task's requested architecture):
//
//   parse command -> parse arguments -> expand parameters -> dispatch
//
// This class is the "expand parameters" stage.
// =================================================================

public final class ParameterExpansion {

    private ParameterExpansion() {
    }

    // =============================================================
    // EXPAND ARGUMENTS
    //
    // Expands every token. A token that consisted ENTIRELY of
    // parameter expansion(s) (no other characters) and whose
    // expanded result is the empty string is dropped from the
    // argument list entirely - it must not become an empty
    // argument. A token with literal text around an expansion
    // (e.g. "${missing}world") keeps its expanded result even if
    // the variable itself was unset/empty, since the surrounding
    // literal text made the overall token non-empty.
    //
    // Tokens with no expansion at all (including an explicit
    // quoted empty string produced upstream by Quoting) are passed
    // through completely unchanged.
    // =============================================================

    public static List<String> expandArguments(List<String> tokens) {

        List<String> result = new ArrayList<>();

        for (String token : tokens) {

            ExpansionResult expansion = expandToken(token);

            if (expansion.hadSubstitution
                    && expansion.value.isEmpty()) {

                // Standalone expansion that evaluated to empty -
                // drop the argument entirely.
                continue;
            }

            result.add(expansion.value);
        }

        return result;
    }

    // =============================================================
    // EXPAND A SINGLE TOKEN
    // =============================================================

    private static ExpansionResult expandToken(String token) {

        StringBuilder result = new StringBuilder();

        boolean substituted = false;

        int i = 0;
        int length = token.length();

        while (i < length) {

            char c = token.charAt(i);

            if (c == '$' && i + 1 < length) {

                char next = token.charAt(i + 1);

                // -----------------------------------------------
                // ${NAME} form
                // -----------------------------------------------

                if (next == '{') {

                    int close = token.indexOf('}', i + 2);

                    if (close != -1) {

                        String name =
                                token.substring(i + 2, close);

                        String value =
                                ShellVariables.get(name);

                        result.append(
                                value == null ? "" : value
                        );

                        substituted = true;

                        i = close + 1;

                        continue;
                    }

                    /*
                     * No closing brace found - treat the '$' as
                     * a literal character and keep scanning.
                     */
                    result.append(c);
                    i++;
                    continue;
                }

                // -----------------------------------------------
                // $NAME form (shell identifier rules: starts with
                // a letter or underscore, then letters/digits/
                // underscores)
                // -----------------------------------------------

                if (Character.isLetter(next) || next == '_') {

                    int j = i + 1;

                    while (j < length) {

                        char candidate = token.charAt(j);

                        if (Character.isLetterOrDigit(candidate)
                                || candidate == '_') {

                            j++;

                        } else {

                            break;
                        }
                    }

                    String name = token.substring(i + 1, j);

                    String value = ShellVariables.get(name);

                    result.append(value == null ? "" : value);

                    substituted = true;

                    i = j;

                    continue;
                }

                /*
                 * '$' not followed by a valid identifier start -
                 * literal '$'.
                 */
                result.append(c);
                i++;
                continue;
            }

            result.append(c);
            i++;
        }

        return new ExpansionResult(result.toString(), substituted);
    }

    // =============================================================
    // RESULT HOLDER
    // =============================================================

    private static final class ExpansionResult {

        final String value;
        final boolean hadSubstitution;

        ExpansionResult(String value, boolean hadSubstitution) {
            this.value = value;
            this.hadSubstitution = hadSubstitution;
        }
    }
}