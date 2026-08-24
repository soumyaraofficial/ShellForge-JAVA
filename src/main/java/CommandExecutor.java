import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CommandExecutor {

    public Path execute(String command, Path currentDirectory) throws Exception {

        List<String> commandSplit = Quoting.parseCommand(command);

        if (commandSplit.isEmpty()) {
            return currentDirectory;
        }

        PrintStream output = System.out;
        PrintStream errorOutput = System.err;

        int redirectIndex = -1;
        String redirectFile = null;

        int errorRedirectIndex = -1;
        String errorRedirectFile = null;

        int appendIndex = -1;
        int errorAppendIndex = -1;

        // Find redirection operators
        for (int i = 0; i < commandSplit.size(); i++) {

            String token = commandSplit.get(i);

            if (token.equals(">") || token.equals("1>")) {

                redirectIndex = i;
                redirectFile = commandSplit.get(i + 1);

            } else if (token.equals("2>")) {

                errorRedirectIndex = i;
                errorRedirectFile = commandSplit.get(i + 1);

            } else if (token.equals(">>") || token.equals("1>>")) {

                appendIndex = i;
                redirectFile = commandSplit.get(i + 1);

            } else if (token.equals("2>>")) {

                errorAppendIndex = i;
                errorRedirectFile = commandSplit.get(i + 1);
            }
        }

        // Find the first redirection operator
        int firstRedirect = commandSplit.size();

        if (redirectIndex != -1) {
            firstRedirect = Math.min(firstRedirect, redirectIndex);
        }

        if (errorRedirectIndex != -1) {
            firstRedirect = Math.min(firstRedirect, errorRedirectIndex);
        }

        if (appendIndex != -1) {
            firstRedirect = Math.min(firstRedirect, appendIndex);
        }

        if (errorAppendIndex != -1) {
            firstRedirect = Math.min(firstRedirect, errorAppendIndex);
        }

        // Arguments without redirection
        List<String> cleanArgs = commandSplit.subList(0, firstRedirect);

        if (cleanArgs.isEmpty()) {
            return currentDirectory;
        }

        // ---------------------------------------------------------
        // BUILT-IN OUTPUT REDIRECTION
        // ---------------------------------------------------------

        if (redirectFile != null) {

            File outFile = new File(redirectFile);

            if (outFile.getParentFile() != null) {
                outFile.getParentFile().mkdirs();
            }

            if (appendIndex != -1) {

                // >>
                output = new PrintStream(
                        new FileOutputStream(outFile, true)
                );

            } else {

                // >
                output = new PrintStream(outFile);
            }
        }

        // ---------------------------------------------------------
        // BUILT-IN ERROR REDIRECTION
        // ---------------------------------------------------------

        if (errorRedirectFile != null) {

            File errFile = new File(errorRedirectFile);

            if (errFile.getParentFile() != null) {
                errFile.getParentFile().mkdirs();
            }

            if (errorAppendIndex != -1) {

                // 2>>
                errorOutput = new PrintStream(
                        new FileOutputStream(errFile, true)
                );

            } else {

                // 2>
                errorOutput = new PrintStream(errFile);
            }
        }

        // ---------------------------------------------------------
        // BUILT-IN COMMANDS
        // ---------------------------------------------------------

        switch (cleanArgs.get(0)) {

            case "echo":

                new Quoting().executeEcho(cleanArgs, output);

                closeRedirectedStreams(output, errorOutput);

                return currentDirectory;

            case "type":

                executeType(cleanArgs, output);

                closeRedirectedStreams(output, errorOutput);

                return currentDirectory;

            case "pwd":

                output.println(currentDirectory);

                closeRedirectedStreams(output, errorOutput);

                return currentDirectory;

            case "cd":

                if (cleanArgs.size() < 2) {

                    errorOutput.println(
                            "cd: missing argument"
                    );

                    closeRedirectedStreams(output, errorOutput);

                    return currentDirectory;
                }

                Path path = executeCd(
                        cleanArgs.get(1),
                        currentDirectory,
                        errorOutput
                );

                closeRedirectedStreams(output, errorOutput);

                return path == null
                        ? currentDirectory
                        : path;
        }

        // ---------------------------------------------------------
        // EXTERNAL COMMANDS
        // ---------------------------------------------------------

        if (getCommandPath(cleanArgs.get(0)) != null) {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(cleanArgs);

            // -----------------------------------------------------
            // STDOUT
            // -----------------------------------------------------

            if (redirectFile != null) {

                File outFile = new File(redirectFile);

                if (appendIndex != -1) {

                    // >>
                    processBuilder.redirectOutput(
                            ProcessBuilder.Redirect.appendTo(outFile)
                    );

                } else {

                    // >
                    processBuilder.redirectOutput(outFile);
                }

            } else {

                processBuilder.redirectOutput(
                        ProcessBuilder.Redirect.INHERIT
                );
            }

            // -----------------------------------------------------
            // STDERR
            // -----------------------------------------------------

            if (errorRedirectFile != null) {

                File errFile = new File(errorRedirectFile);

                if (errorAppendIndex != -1) {

                    // 2>>
                    processBuilder.redirectError(
                            ProcessBuilder.Redirect.appendTo(errFile)
                    );

                } else {

                    // 2>
                    processBuilder.redirectError(errFile);
                }

            } else {

                processBuilder.redirectError(
                        ProcessBuilder.Redirect.INHERIT
                );
            }

            Process process = processBuilder.start();

            process.waitFor();

        } else {

            // Command not found goes to stderr
            errorOutput.println(
                    cleanArgs.get(0) + ": command not found"
            );
        }

        closeRedirectedStreams(output, errorOutput);

        return currentDirectory;
    }

    // -------------------------------------------------------------
    // CLOSE REDIRECTED STREAMS
    // -------------------------------------------------------------

    private void closeRedirectedStreams(
            PrintStream output,
            PrintStream errorOutput) {

        if (output != System.out) {
            output.close();
        }

        if (errorOutput != System.err) {
            errorOutput.close();
        }
    }

    // -------------------------------------------------------------
    // CD COMMAND
    // -------------------------------------------------------------

    private Path executeCd(
            String command,
            Path currentDirectory,
            PrintStream errorOutput) {

        // cd ~
        if (command.equals("~")) {

            String home = System.getenv("HOME");

            if (home == null) {
                errorOutput.println(
                        "cd: HOME not set"
                );

                return null;
            }

            return Path.of(home);
        }

        Path path = Path.of(command);

        // Relative path
        if (!path.isAbsolute()) {

            path = currentDirectory
                    .resolve(command)
                    .normalize();
        }

        // Directory doesn't exist
        if (!Files.isDirectory(path)) {

            errorOutput.println(
                    "cd: " + command
                            + ": No such file or directory"
            );

            return null;
        }

        return path;
    }

    // -------------------------------------------------------------
    // TYPE COMMAND
    // -------------------------------------------------------------

    private void executeType(
            List<String> args,
            PrintStream output) {

        if (args.size() < 2) {

            output.println(
                    "type: missing argument"
            );

            return;
        }

        String search = args.get(1);

        output.println(
                search + type(search)
        );
    }

    // -------------------------------------------------------------
    // CHECK BUILTIN OR EXTERNAL COMMAND
    // -------------------------------------------------------------

    public static String type(String command) {

        String[] builtin = {
                "exit",
                "echo",
                "type",
                "pwd",
                "cd"
        };

        for (String s : builtin) {

            if (s.equalsIgnoreCase(command)) {
                return " is a shell builtin";
            }
        }

        String path = getCommandPath(command);

        if (path != null) {
            return " is " + path;
        }

        return ": not found";
    }

    // -------------------------------------------------------------
    // GET EXTERNAL COMMAND PATH
    // -------------------------------------------------------------

    public static String getCommandPath(String command) {

        String paths = System.getenv("PATH");

        if (paths == null) {
            return null;
        }

        String[] pathDirs =
                paths.split(File.pathSeparator);

        for (String dir : pathDirs) {

            File file = new File(dir, command);

            if (file.exists() && file.canExecute()) {

                return file.getAbsolutePath();
            }
        }

        return null;
    }
}