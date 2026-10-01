import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class AIBuiltin {

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";

    private static final String MODEL = "qwen2.5:3b";

    private static final String SYSTEM_PROMPT = """
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

    public void execute(
            List<String> args,
            java.io.PrintStream output,
            java.io.PrintStream errorOutput) {

        if (args.size() < 2) {
            errorOutput.println("ai: missing question");
            return;
        }

        StringBuilder question = new StringBuilder();

        for (int i = 1; i < args.size(); i++) {

            if (i > 1) {
                question.append(" ");
            }

            question.append(args.get(i));
        }

        try {

            String response =
                    askOllama(question.toString());

            output.println("AI: " + response);

        } catch (Exception e) {

            errorOutput.println(
                    "ai: unable to connect to Ollama: "
                            + e.getMessage()
            );
        }
    }

    private String askOllama(String question)
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
                escapeJson(SYSTEM_PROMPT),
                escapeJson(question)
        );

        HttpClient client =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(OLLAMA_URL))
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

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString()
                );

        if (response.statusCode() != 200) {

            throw new IOException(
                    "Ollama returned HTTP "
                            + response.statusCode()
            );
        }

        return extractResponse(
                response.body()
        ).trim();
    }

    private String extractResponse(String json)
            throws IOException {

        String marker = "\"response\":\"";

        int start = json.indexOf(marker);

        if (start == -1) {

            throw new IOException(
                    "invalid response from Ollama"
            );
        }

        start += marker.length();

        StringBuilder result =
                new StringBuilder();

        boolean escaped = false;

        for (int i = start;
             i < json.length();
             i++) {

            char c = json.charAt(i);

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

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}