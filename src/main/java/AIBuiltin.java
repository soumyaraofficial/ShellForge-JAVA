import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.jline.reader.LineReader;

public class AIBuiltin {

    private final CommandExecutor executor;

    public AIBuiltin(CommandExecutor executor) {
        this.executor = executor;
    }

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";

    private static final String MODEL =
            "qwen2.5:3b";


    // =============================================================
    // COMMAND GENERATION PROMPT
    // =============================================================

    private static final String COMMAND_PROMPT = """
            You are an AI assistant inside a Unix-like shell.

            The user will describe an operation they want to perform.

            Return ONLY the shell command that performs the operation.
            Do not explain the command.
            Do not use markdown.
            Do not use code fences.

            Examples:

            User: add mango to temp.txt
            Output: echo "mango" >> temp.txt

            User: create a file called hello.txt
            Output: touch hello.txt

            User: show files in the current directory
            Output: ls
            """;


    // =============================================================
    // EXPLANATION PROMPT
    // =============================================================

    private static final String EXPLAIN_PROMPT = """
            You are an AI assistant inside a Unix-like shell.

            The user will provide a shell command.

            Explain what the command does in a clear and concise way.

            Explain:
            1. What the command does.
            2. What each important argument or option means.
            3. What the expected result is.

            Do not execute the command.
            Do not suggest a different command.
            Do not use excessive explanation.
            Use simple language.

            Example:

            Command:
            find . -name "hello.txt"

            Explanation:
            The find command searches for files and directories.
            The . means to start searching from the current directory.
            -name "hello.txt" searches for an exact name match.
            """;


    // =============================================================
    // FIX PROMPT
    // =============================================================

    private static final String FIX_PROMPT = """
            You are an AI assistant inside a Unix-like shell.

            The user will provide:
            1. The shell command that failed.
            2. The error message produced by that command.

            Diagnose the problem and suggest the correct fix.

            Your response MUST contain exactly:

            Problem:
            <short explanation of what went wrong>

            Fix:
            <correct shell command>

            Do not execute the command.
            Do not use markdown code fences.
            Keep the explanation concise.

            Example:

            Command:
            mkdir test

            Error:
            mkdir: test: File exists

            Response:

            Problem:
            The directory "test" already exists.

            Fix:
            mkdir -p test
            """;


    // =============================================================
    // MAIN AI BUILTIN
    // =============================================================

    public void execute(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput,
            Path currentDirectory,
            LineReader reader) {

        // =========================================================
        // CHECK ARGUMENT
        // =========================================================

        if (args.size() < 2) {

            errorOutput.println(
                    "ai: missing question"
            );

            return;
        }


        // =========================================================
        // CHECK MODE
        //
        // ai <question>
        // ai execute <question>
        // ai explain <command>
        // ai fix
        // =========================================================

        String mode =
                args.get(1);


        // =========================================================
        // EXPLAIN MODE
        // =========================================================

        if (mode.equalsIgnoreCase("explain")) {

            executeExplain(
                    args,
                    output,
                    errorOutput
            );

            return;
        }


        // =========================================================
        // FIX MODE
        // =========================================================

        if (mode.equalsIgnoreCase("fix")) {

            executeFix(
                    output,
                    errorOutput
            );

            return;
        }


        // =========================================================
        // EXECUTE MODE
        // =========================================================

        boolean executeCommand =
                mode.equalsIgnoreCase("execute");


        // =========================================================
        // BUILD QUESTION
        // =========================================================

        StringBuilder question =
                new StringBuilder();

        int questionStart =
                executeCommand ? 2 : 1;


        if (args.size() <= questionStart) {

            errorOutput.println(
                    "ai: missing question"
            );

            return;
        }


        for (
                int i = questionStart;
                i < args.size();
                i++
        ) {

            if (i > questionStart) {
                question.append(" ");
            }

            question.append(
                    args.get(i)
            );
        }


        // =========================================================
        // ASK OLLAMA FOR COMMAND
        // =========================================================

        try {

            String response =
                    askOllama(
                            COMMAND_PROMPT,
                            question.toString()
                    ).trim();


            // =====================================================
            // SHOW AI SUGGESTION
            // =====================================================

            output.println();

            output.println(
                    "AI suggestion:"
            );

            output.println(
                    response
            );

            output.println();


            // =====================================================
            // NORMAL "ai" MODE
            //
            // Only suggest.
            // Never execute.
            // =====================================================

            if (!executeCommand) {

                return;
            }


            // =====================================================
            // "ai execute" MODE
            //
            // Ask for confirmation.
            // =====================================================

            output.print(
                    "Execute? [y/N] "
            );

            output.flush();

            String answer =
                    reader.readLine();


            // =====================================================
            // CANCEL
            // =====================================================

            if (
                    answer == null
                            || !answer.equalsIgnoreCase("y")
            ) {

                output.println(
                        "Cancelled."
                );

                return;
            }


            // =====================================================
            // EXECUTE APPROVED COMMAND
            // =====================================================

            output.println(
                    "Executing..."
            );

            executor.execute(
                    response,
                    currentDirectory
            );

            output.println(
                    "Executed"
            );


        } catch (Exception e) {

            errorOutput.println(
                    "ai: " + e.getMessage()
            );
        }
    }


    // =============================================================
    // EXPLAIN COMMAND
    // =============================================================

    private void executeExplain(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput) {

        // =========================================================
        // CHECK COMMAND
        // =========================================================

        if (args.size() < 3) {

            errorOutput.println(
                    "ai explain: missing command"
            );

            return;
        }


        // =========================================================
        // BUILD COMMAND STRING
        // =========================================================

        StringBuilder command =
                new StringBuilder();


        for (
                int i = 2;
                i < args.size();
                i++
        ) {

            if (i > 2) {
                command.append(" ");
            }

            command.append(
                    args.get(i)
            );
        }


        // =========================================================
        // ASK OLLAMA
        // =========================================================

        try {

            String explanation =
                    askOllama(
                            EXPLAIN_PROMPT,
                            command.toString()
                    ).trim();


            // =====================================================
            // SHOW EXPLANATION
            // =====================================================

            output.println();

            output.println(
                    "Explanation:"
            );

            output.println(
                    explanation
            );

            output.println();


        } catch (Exception e) {

            errorOutput.println(
                    "ai explain: "
                            + e.getMessage()
            );
        }
    }


    // =============================================================
    // FIX COMMAND
    // =============================================================

    private void executeFix(
            PrintStream output,
            PrintStream errorOutput) {

        // =========================================================
        // GET LAST COMMAND
        // =========================================================

        String lastCommand =
                executor.getLastCommand();


        // =========================================================
        // GET LAST ERROR
        // =========================================================

        String lastError =
                executor.getLastError();


        // =========================================================
        // CHECK COMMAND
        // =========================================================

        if (
                lastCommand == null
                        || lastCommand.isBlank()
        ) {

            errorOutput.println(
                    "ai fix: no previous command found"
            );

            return;
        }


        // =========================================================
        // CHECK ERROR
        // =========================================================

        if (
                lastError == null
                        || lastError.isBlank()
        ) {

            errorOutput.println(
                    "ai fix: no error found"
            );

            return;
        }


        // =========================================================
        // BUILD AI PROMPT
        // =========================================================

        String prompt = """
                Command:
                %s

                Error:
                %s
                """.formatted(
                lastCommand,
                lastError
        );


        // =========================================================
        // ASK OLLAMA
        // =========================================================

        try {

            String response =
                    askOllama(
                            FIX_PROMPT,
                            prompt
                    ).trim();


            // =====================================================
            // SHOW AI DIAGNOSIS
            // =====================================================

            output.println();

            output.println(
                    "AI diagnosis:"
            );

            output.println(
                    response
            );

            output.println();


        } catch (Exception e) {

            errorOutput.println(
                    "ai fix: "
                            + e.getMessage()
            );
        }
    }


    // =============================================================
    // ASK OLLAMA
    // =============================================================

    private String askOllama(
            String systemPrompt,
            String prompt)
            throws Exception {

        String json = """
                {
                  "model": "%s",
                  "system": "%s",
                  "prompt": "%s",
                  "stream": false,
                  "think": false
                }
                """.formatted(
                MODEL,
                escapeJson(systemPrompt),
                escapeJson(prompt)
        );


        // =========================================================
        // HTTP CLIENT
        // =========================================================

        HttpClient client =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();


        // =========================================================
        // HTTP REQUEST
        // =========================================================

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        OLLAMA_URL
                                )
                        )
                        .timeout(
                                Duration.ofMinutes(2)
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();


        // =========================================================
        // SEND REQUEST
        // =========================================================

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString()
                );


        // =========================================================
        // CHECK RESPONSE
        // =========================================================

        if (response.statusCode() != 200) {

            throw new IOException(
                    "Ollama returned HTTP "
                            + response.statusCode()
            );
        }


        // =========================================================
        // EXTRACT RESPONSE
        // =========================================================

        return extractResponse(
                response.body()
        ).trim();
    }


    // =============================================================
    // EXTRACT "response" FROM OLLAMA JSON
    // =============================================================

    private String extractResponse(
            String json)
            throws IOException {

        String marker =
                "\"response\":\"";


        int start =
                json.indexOf(marker);


        if (start == -1) {

            throw new IOException(
                    "invalid response from Ollama"
            );
        }


        start += marker.length();


        StringBuilder result =
                new StringBuilder();

        boolean escaped = false;


        for (
                int i = start;
                i < json.length();
                i++
        ) {

            char c =
                    json.charAt(i);


            // =====================================================
            // ESCAPED CHARACTER
            // =====================================================

            if (escaped) {

                switch (c) {

                    case 'n':
                        result.append('\n');
                        break;

                    case 'r':
                        result.append('\r');
                        break;

                    case 't':
                        result.append('\t');
                        break;

                    case '"':
                        result.append('"');
                        break;

                    case '\\':
                        result.append('\\');
                        break;

                    case 'u':

                        if (i + 4 < json.length()) {

                            String unicode =
                                    json.substring(i + 1, i + 5);

                            try {

                                char decoded =
                                        (char) Integer.parseInt(
                                                unicode,
                                                16
                                        );

                                result.append(decoded);

                                i += 4;

                            } catch (NumberFormatException e) {

                                result.append('u');
                            }

                        } else {

                            result.append('u');
                        }

                        break;

                    default:
                        result.append(c);
                }

                escaped = false;


            } else if (c == '\\') {

                escaped = true;


            } else if (c == '"') {

                break;


            } else {

                result.append(c);
            }
        }


        return result.toString();
    }


    // =============================================================
    // ESCAPE JSON STRING
    // =============================================================

    private String escapeJson(
            String value) {

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\t",
                        "\\t"
                );
    }
}