import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class ProjectBuiltin {

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";

    private static final String MODEL =
            "qwen2.5:3b";

    public static void execute(Path currentDirectory) {

        ProjectScanner.ProjectInfo info =
                ProjectScanner.scan(currentDirectory);

        System.out.println();

        System.out.println("╭──────────── PROJECT ────────────╮");

        System.out.printf(
                "│ Name       : %-20s│%n",
                info.getName()
        );

        System.out.printf(
                "│ Type       : %-20s│%n",
                info.getProjectType()
        );

        System.out.printf(
                "│ Language   : %-20s│%n",
                info.getPrimaryLanguage()
        );

        System.out.printf(
                "│ Build      : %-20s│%n",
                info.getBuildTool()
        );

        System.out.printf(
                "│ Git        : %-20s│%n",
                info.isGit() ? "✓" : "✗"
        );

        System.out.printf(
                "│ README     : %-20s│%n",
                info.hasReadme() ? "✓" : "✗"
        );

        System.out.printf(
                "│ Source     : %-20s│%n",
                info.getSourceDirectory()
        );

        System.out.printf(
                "│ Tests      : %-20s│%n",
                info.getTestDirectory()
        );

        System.out.printf(
                "│ Files      : %-20d│%n",
                info.getTotalFiles()
        );

        System.out.println("╰────────────────────────────────╯");

        System.out.println();

        System.out.println("Languages:");

        info.getLanguages()
                .entrySet()
                .stream()
                .sorted(
                        (a, b) ->
                                Integer.compare(
                                        b.getValue(),
                                        a.getValue()
                                )
                )
                .forEach(entry ->
                        System.out.printf(
                                "  %-20s %d files%n",
                                entry.getKey(),
                                entry.getValue()
                        )
                );

        System.out.println();
    }


    public static void analyze(Path currentDirectory) {

        ProjectScanner.ProjectInfo info =
                ProjectScanner.scan(currentDirectory);

        System.out.println();
        System.out.println("Analyzing project with AI...");
        System.out.println();

        String projectContext =
                buildProjectContext(info);

        String prompt =
                """
                Analyze this software project.

                Project information:
                
                %s

                Provide a concise developer-focused analysis.

                Include:
                1. What type of project this appears to be
                2. Main technologies
                3. Likely architecture
                4. Build system
                5. Testing setup
                6. A short description of what the project is likely designed to do

                Do not invent files, technologies, frameworks,
                or functionality that are not supported by the
                provided project information.

                Keep the answer concise and useful for a developer.
                """.formatted(projectContext);

        try {

            String response = askOllama(prompt);

            System.out.println(cleanMarkdown(response));
            System.out.println();

        } catch (Exception e) {

            System.err.println(
                    "AI analysis failed: "
                            + e.getMessage()
            );
        }
    }


    private static String buildProjectContext(
            ProjectScanner.ProjectInfo info) {

        StringBuilder context =
                new StringBuilder();

        context.append("Name: ")
                .append(info.getName())
                .append("\n");

        context.append("Project Type: ")
                .append(info.getProjectType())
                .append("\n");

        context.append("Primary Language: ")
                .append(info.getPrimaryLanguage())
                .append("\n");

        context.append("Build Tool: ")
                .append(info.getBuildTool())
                .append("\n");

        context.append("Git Repository: ")
                .append(info.isGit())
                .append("\n");

        context.append("README: ")
                .append(info.hasReadme())
                .append("\n");

        context.append("Source Directory: ")
                .append(info.getSourceDirectory())
                .append("\n");

        context.append("Test Directory: ")
                .append(info.getTestDirectory())
                .append("\n");

        context.append("Total Files: ")
                .append(info.getTotalFiles())
                .append("\n");

        context.append("Source Files: ")
                .append(info.getSourceFiles())
                .append("\n");

        context.append("Languages:\n");

        for (Map.Entry<String, Integer> entry :
                info.getLanguages().entrySet()) {

            context.append("  ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" files\n");
        }

        return context.toString();
    }


    private static String askOllama(
            String prompt) throws Exception {

        String json =
                "{"
                        + "\"model\":\"" + MODEL + "\","
                        + "\"prompt\":\""
                        + escapeJson(prompt)
                        + "\","
                        + "\"stream\":false"
                        + "}";

        HttpClient client =
                HttpClient.newHttpClient();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(OLLAMA_URL))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        json,
                                        StandardCharsets.UTF_8
                                )
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ollama returned HTTP "
                            + response.statusCode()
            );
        }

        return extractResponse(response.body());
    }


    private static String extractResponse(
            String json) {

        String marker =
                "\"response\":\"";

        int start =
                json.indexOf(marker);

        if (start == -1) {
            return json;
        }

        start += marker.length();

        int end = start;

        boolean escaped = false;

        while (end < json.length()) {

            char c = json.charAt(end);

            if (c == '"' && !escaped) {
                break;
            }

            if (c == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }

            end++;
        }

        String response =
                json.substring(start, end);

        return response
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }


    private static String escapeJson(
            String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    private static String cleanMarkdown(String text) {

        return text
                .replace("**", "")
                .replace("__", "")
                .replace("```", "")
                .replace("`", "");
    }
}