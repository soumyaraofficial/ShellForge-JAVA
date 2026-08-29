import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CommandExecutor {

    public Path execute(
            String command,
            Path currentDirectory) throws Exception {

        List<String> commandSplit =
                Quoting.parseCommand(command);

        if (commandSplit.isEmpty()) {
            return currentDirectory;
        }

        // =========================================================
        // BACKGROUND EXECUTION (&)
        //
        // Only the real tokenizer's output is inspected here (not
        // a naive string split), so existing quoting behavior is
        // fully preserved. Stripped before redirection scanning so
        // nothing downstream ever sees the "&" token.
        // =========================================================

        boolean background = false;

        if (commandSplit.get(commandSplit.size() - 1).equals("&")) {

            background = true;

            commandSplit.remove(commandSplit.size() - 1);
        }

        if (commandSplit.isEmpty()) {
            return currentDirectory;
        }

        String displayCommand =
                String.join(" ", commandSplit);

        PrintStream output = System.out;
        PrintStream errorOutput = System.err;

        int redirectIndex = -1;
        String redirectFile = null;

        int errorRedirectIndex = -1;
        String errorRedirectFile = null;

        int appendIndex = -1;
        int errorAppendIndex = -1;

        // =========================================================
        // FIND REDIRECTION OPERATORS
        // =========================================================

        for (int i = 0; i < commandSplit.size(); i++) {

            String token = commandSplit.get(i);

            // -----------------------------------------------------
            // STDOUT REDIRECTION
            // -----------------------------------------------------

            if (token.equals(">") || token.equals("1>")) {

                redirectIndex = i;

                if (i + 1 < commandSplit.size()) {
                    redirectFile = commandSplit.get(i + 1);
                }
            }

            // -----------------------------------------------------
            // STDERR REDIRECTION
            // -----------------------------------------------------

            else if (token.equals("2>")) {

                errorRedirectIndex = i;

                if (i + 1 < commandSplit.size()) {
                    errorRedirectFile =
                            commandSplit.get(i + 1);
                }
            }

            // -----------------------------------------------------
            // STDOUT APPEND
            // -----------------------------------------------------

            else if (token.equals(">>")
                    || token.equals("1>>")) {

                appendIndex = i;

                if (i + 1 < commandSplit.size()) {
                    redirectFile =
                            commandSplit.get(i + 1);
                }
            }

            // -----------------------------------------------------
            // STDERR APPEND
            // -----------------------------------------------------

            else if (token.equals("2>>")) {

                errorAppendIndex = i;

                if (i + 1 < commandSplit.size()) {
                    errorRedirectFile =
                            commandSplit.get(i + 1);
                }
            }
        }

        // =========================================================
        // FIND FIRST REDIRECTION
        // =========================================================

        int firstRedirect = commandSplit.size();

        if (redirectIndex != -1) {

            firstRedirect =
                    Math.min(firstRedirect, redirectIndex);
        }

        if (errorRedirectIndex != -1) {

            firstRedirect =
                    Math.min(firstRedirect, errorRedirectIndex);
        }

        if (appendIndex != -1) {

            firstRedirect =
                    Math.min(firstRedirect, appendIndex);
        }

        if (errorAppendIndex != -1) {

            firstRedirect =
                    Math.min(firstRedirect, errorAppendIndex);
        }

        // =========================================================
        // REMOVE REDIRECTION PART
        // =========================================================

        List<String> cleanArgs =
                commandSplit.subList(0, firstRedirect);

        if (cleanArgs.isEmpty()) {
            return currentDirectory;
        }

        // =========================================================
        // STDOUT REDIRECTION FOR BUILT-INS
        // =========================================================

        if (redirectFile != null) {

            File outFile =
                    new File(redirectFile);

            if (outFile.getParentFile() != null) {

                outFile.getParentFile().mkdirs();
            }

            if (appendIndex != -1) {

                output = new PrintStream(
                        new FileOutputStream(
                                outFile,
                                true
                        )
                );

            } else {

                output = new PrintStream(outFile);
            }
        }

        // =========================================================
        // STDERR REDIRECTION FOR BUILT-INS
        // =========================================================

        if (errorRedirectFile != null) {

            File errFile =
                    new File(errorRedirectFile);

            if (errFile.getParentFile() != null) {

                errFile.getParentFile().mkdirs();
            }

            if (errorAppendIndex != -1) {

                errorOutput = new PrintStream(
                        new FileOutputStream(
                                errFile,
                                true
                        )
                );

            } else {

                errorOutput =
                        new PrintStream(errFile);
            }
        }

        // =========================================================
        // BUILT-IN COMMANDS
        // =========================================================

        switch (cleanArgs.get(0)) {

            // -----------------------------------------------------
            // ECHO
            // -----------------------------------------------------

            case "echo":

                new Quoting().executeEcho(
                        cleanArgs,
                        output
                );

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            // -----------------------------------------------------
            // TYPE
            // -----------------------------------------------------

            case "type":

                executeType(
                        cleanArgs,
                        output
                );

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            // -----------------------------------------------------
            // PWD
            // -----------------------------------------------------

            case "pwd":

                output.println(currentDirectory);

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            // -----------------------------------------------------
            // CD
            // -----------------------------------------------------

            case "cd":

                if (cleanArgs.size() < 2) {

                    errorOutput.println(
                            "cd: missing argument"
                    );

                    closeRedirectedStreams(
                            output,
                            errorOutput
                    );

                    return currentDirectory;
                }

                Path path =
                        executeCd(
                                cleanArgs.get(1),
                                currentDirectory,
                                errorOutput
                        );

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return path == null
                        ? currentDirectory
                        : path;

            // -----------------------------------------------------
            // COMPLETE
            // -----------------------------------------------------

            case "complete":

                new CompleteBuiltin().execute(
                        cleanArgs,
                        output,
                        errorOutput
                );

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            // -----------------------------------------------------
            // JOBS
            // -----------------------------------------------------

            case "jobs":

                new JobsBuiltin().execute(output);

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;
        }

        // =========================================================
        // EXTERNAL COMMANDS
        // =========================================================

        if (getCommandPath(cleanArgs.get(0)) != null) {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(cleanArgs);

            // -----------------------------------------------------
            // STDOUT
            // -----------------------------------------------------

            if (redirectFile != null) {

                File outFile =
                        new File(redirectFile);

                if (appendIndex != -1) {

                    processBuilder.redirectOutput(
                            ProcessBuilder.Redirect.appendTo(
                                    outFile
                            )
                    );

                } else {

                    processBuilder.redirectOutput(
                            outFile
                    );
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

                File errFile =
                        new File(errorRedirectFile);

                if (errorAppendIndex != -1) {

                    processBuilder.redirectError(
                            ProcessBuilder.Redirect.appendTo(
                                    errFile
                            )
                    );

                } else {

                    processBuilder.redirectError(
                            errFile
                    );
                }

            } else {

                processBuilder.redirectError(
                        ProcessBuilder.Redirect.INHERIT
                );
            }

            Process process =
                    processBuilder.start();

            // -----------------------------------------------------
            // BACKGROUND vs FOREGROUND
            //
            // Background: register the job and print "[n] pid"
            // immediately, without waiting - the shell-level
            // announcement always goes to the real stdout, never
            // to a redirected `output` stream, matching how job
            // control messages work regardless of the command's
            // own redirections.
            //
            // Foreground: unchanged - wait exactly as before.
            // -----------------------------------------------------

            if (background) {

                JobManager.Job job =
                        JobManager.startJob(
                                process,
                                displayCommand
                        );

                System.out.println(
                        "[" + job.jobNumber + "] " + process.pid()
                );

            } else {

                process.waitFor();
            }

        } else {

            // -----------------------------------------------------
            // COMMAND NOT FOUND
            // -----------------------------------------------------

            errorOutput.println(
                    cleanArgs.get(0)
                            + ": command not found"
            );
        }

        // =========================================================
        // CLOSE REDIRECTED STREAMS
        // =========================================================

        closeRedirectedStreams(
                output,
                errorOutput
        );

        return currentDirectory;
    }

    // =============================================================
    // CLOSE REDIRECTED STREAMS
    // =============================================================

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

    // =============================================================
    // CD COMMAND
    // =============================================================

    private Path executeCd(
            String command,
            Path currentDirectory,
            PrintStream errorOutput) {

        // ---------------------------------------------------------
        // cd ~
        // ---------------------------------------------------------

        if (command.equals("~")) {

            String home =
                    System.getenv("HOME");

            if (home == null) {

                errorOutput.println(
                        "cd: HOME not set"
                );

                return null;
            }

            return Path.of(home);
        }

        // ---------------------------------------------------------
        // CREATE PATH
        // ---------------------------------------------------------

        Path path =
                Path.of(command);

        // ---------------------------------------------------------
        // RELATIVE PATH
        // ---------------------------------------------------------

        if (!path.isAbsolute()) {

            path = currentDirectory
                    .resolve(command)
                    .normalize();
        }

        // ---------------------------------------------------------
        // DIRECTORY DOES NOT EXIST
        // ---------------------------------------------------------

        if (!Files.isDirectory(path)) {

            errorOutput.println(
                    "cd: "
                            + command
                            + ": No such file or directory"
            );

            return null;
        }

        return path;
    }

    // =============================================================
    // TYPE COMMAND
    // =============================================================

    private void executeType(
            List<String> args,
            PrintStream output) {

        if (args.size() < 2) {

            output.println(
                    "type: missing argument"
            );

            return;
        }

        String search =
                args.get(1);

        output.println(
                search
                        + type(search)
        );
    }

    // =============================================================
    // CHECK BUILT-IN OR EXTERNAL COMMAND
    // =============================================================

    public static String type(
            String command) {

        if (Builtins.isBuiltin(command)) {
            return " is a shell builtin";
        }

        String path =
                getCommandPath(command);

        if (path != null) {

            return " is " + path;
        }

        return ": not found";
    }

    // =============================================================
    // GET EXTERNAL COMMAND PATH
    // =============================================================

    public static String getCommandPath(
            String command) {

        String paths =
                System.getenv("PATH");

        if (paths == null) {
            return null;
        }

        String[] pathDirs =
                paths.split(File.pathSeparator);

        for (String dir : pathDirs) {

            File file =
                    new File(dir, command);

            if (file.exists()
                    && file.canExecute()) {

                return file.getAbsolutePath();
            }
        }

        return null;
    }
}