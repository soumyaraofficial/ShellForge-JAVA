
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jline.reader.LineReader;

public class CommandExecutor {
    private String lastCommand;
    private String lastError;
    private LineReader reader;

    public void setReader(LineReader reader) {
        this.reader = reader;
    }
    public Path execute(
            String command,
            Path currentDirectory) throws Exception {
        if (!command.trim().equalsIgnoreCase("ai fix")) {
            lastCommand = command;
            lastError = null;
        }

        List<String> commandSplit =
                Quoting.parseCommand(command);

        if (commandSplit.isEmpty()) {
            return currentDirectory;
        }

        // =========================================================
        // PARAMETER EXPANSION ($NAME / ${NAME})
        //
        // Runs on the already-tokenized command, right after
        // parsing/quoting and before anything else (pipe/background
        // detection, redirection parsing, builtin dispatch, and
        // external execution) looks at the tokens - so expanded
        // values are available everywhere downstream exactly as if
        // they'd been typed literally, while quoting/escaping rules
        // above remain completely untouched.
        // =========================================================

        commandSplit =
                ParameterExpansion.expandArguments(commandSplit);

        if (commandSplit.isEmpty()) {
            return currentDirectory;
        }

        // =========================================================
        // PIPELINES (|)
        //
        // Detected purely from the already-tokenized command, so
        // existing quoting/escaping behavior (a quoted "|" becomes
        // part of a normal argument, never a bare "|" token) is
        // preserved automatically. Only a standalone, whitespace-
        // separated "|" token is ever treated as a pipe operator.
        //
        // Everything below this point is completely unchanged and
        // continues to handle the non-pipeline case exactly as it
        // did before pipeline support was added.
        // =========================================================

        if (containsPipe(commandSplit)) {
            return executePipeline(commandSplit, currentDirectory);
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

            // -----------------------------------------------------
            // HISTORY
            // -----------------------------------------------------

            case "history":

                new HistoryBuiltin().execute(
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
            // DECLARE
            // -----------------------------------------------------

            case "declare":

                new DeclareBuiltin().execute(
                        cleanArgs,
                        output,
                        errorOutput
                );

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            //----------------------------------------------------
            // AI
            // ------------------------------
            case "ai":

                new AIBuiltin(this).execute(
                        cleanArgs,
                        output,
                        errorOutput,
                        currentDirectory,
                        reader
                );
                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            case "project":

                if (cleanArgs.size() > 1
                        && cleanArgs.get(1).equalsIgnoreCase("analyze")) {

                    ProjectBuiltin.analyze(currentDirectory);

                } else {

                    ProjectBuiltin.execute(currentDirectory);
                }

                return currentDirectory;


            case "inspect":

                if (cleanArgs.size() < 2) {

                    errorOutput.println(
                            "inspect: missing file or directory"
                    );

                    closeRedirectedStreams(
                            output,
                            errorOutput
                    );

                    return currentDirectory;
                }

                Path inspectPath = currentDirectory
                        .resolve(cleanArgs.get(1))
                        .normalize();

                InspectBuiltin.execute(inspectPath);

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            case "tree":

                Path treeDirectory = currentDirectory;
                int depth = 5;

                // tree 2
                if (cleanArgs.size() > 1
                        && cleanArgs.get(1).matches("\\d+")) {

                    depth = Integer.parseInt(cleanArgs.get(1));

                }

                // tree src
                else if (cleanArgs.size() > 1) {

                    String pathArg = cleanArgs.get(1);

                    treeDirectory = currentDirectory
                            .resolve(pathArg)
                            .normalize();

                    // tree src 2
                    if (cleanArgs.size() > 2) {

                        String depthArg = cleanArgs.get(2);

                        if (!depthArg.matches("\\d+")) {

                            errorOutput.println(
                                    "tree: depth must be a number"
                            );

                            closeRedirectedStreams(
                                    output,
                                    errorOutput
                            );

                            return currentDirectory;
                        }

                        depth = Integer.parseInt(depthArg);
                    }
                }

                if (!Files.isDirectory(treeDirectory)) {

                    errorOutput.println(
                            "tree: directory not found: "
                                    + treeDirectory
                    );

                    closeRedirectedStreams(
                            output,
                            errorOutput
                    );

                    return currentDirectory;
                }

                TreeBuiltin.execute(
                        treeDirectory,
                        depth
                );

                closeRedirectedStreams(
                        output,
                        errorOutput
                );

                return currentDirectory;

            case "findx":

                if (cleanArgs.size() < 2) {

                    errorOutput.println(
                            "findx: missing search term"
                    );

                    closeRedirectedStreams(
                            output,
                            errorOutput
                    );

                    return currentDirectory;
                }

                String searchTerm = cleanArgs.get(1);

                Path searchDirectory = currentDirectory;

                if (cleanArgs.size() > 2) {

                    searchDirectory = currentDirectory
                            .resolve(cleanArgs.get(2))
                            .normalize();
                }

                if (!Files.isDirectory(searchDirectory)) {

                    errorOutput.println(
                            "findx: directory not found: "
                                    + searchDirectory
                    );

                    closeRedirectedStreams(
                            output,
                            errorOutput
                    );

                    return currentDirectory;
                }

                FindxBuiltin.execute(
                        searchDirectory,
                        searchTerm
                );

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

            // Run the external command from the shell's
            // current working directory.
            processBuilder.directory(
                    currentDirectory.toFile()
            );

            // -----------------------------------------------------
            // STDIN
            //
            // Previously left at the ProcessBuilder default
            // (Redirect.PIPE), which connects the child's stdin to
            // a pipe the shell never writes to or closes - so the
            // child never sees real terminal input, and typing
            // Ctrl+D at the terminal has no effect on it (that EOF
            // only applies to the shell's own stdin/JLine reader).
            //
            // A foreground external command not part of a pipeline
            // has nothing else feeding its stdin, so it must
            // inherit the real terminal stdin directly, exactly
            // like stdout/stderr already do below when they aren't
            // redirected.
            //
            // Background ("&") commands are left as they were -
            // this fix only concerns foreground stdin handling.
            // -----------------------------------------------------

            if (!background) {

                processBuilder.redirectInput(
                        ProcessBuilder.Redirect.INHERIT
                );
            }

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


            lastError =
                    cleanArgs.get(0)
                            + ": command not found";

            errorOutput.println(lastError);

            settleConsole(errorOutput);
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

    // =================================================================
    // SETTLE CONSOLE
    //
    // IDE consoles read stdout and stderr through separate pipes, so a
    // message written to System.err can show up AFTER the next prompt
    // (which JLine writes to stdout). Flush both streams and give the
    // console a brief moment to render the message before the prompt
    // is drawn. Only applies to the real console, not redirected files.
    // =================================================================

    private static void settleConsole(PrintStream stream) {

        stream.flush();

        if (stream != System.err) {
            return;
        }

        System.out.flush();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =================================================================
    // =================================================================
    // PIPELINE SUPPORT
    //
    // Everything below is new. Nothing above this banner (aside from
    // the 4-line dispatch added at the very top of execute()) was
    // changed to add this.
    //
    // Design:
    //  - Stages are split from the tokens already produced by
    //    Quoting.parseCommand(), so quoting/escaping rules are
    //    inherited for free.
    //  - Every stage is validated (command exists / stage isn't
    //    empty) BEFORE any process is started, so a bad pipeline
    //    never leaves a partially-started chain hanging.
    //  - External stages are plain ProcessBuilder processes, wired
    //    to their neighbors either directly (external -> external,
    //    via a byte-pumping daemon thread) or via captured output
    //    (builtin -> external / external -> builtin, since none of
    //    this shell's builtins ever read stdin).
    //  - Only the LAST stage is waited for on the calling thread.
    //    Earlier external stages are reaped on background daemon
    //    threads instead, so a producer like "tail -f" can never
    //    block the shell from returning to the prompt once the
    //    downstream stage (e.g. "head") has finished.
    // =================================================================
    // =================================================================

    private boolean containsPipe(List<String> tokens) {

        for (String token : tokens) {

            if (token.equals("|")) {
                return true;
            }
        }

        return false;
    }

    private List<List<String>> splitIntoStages(List<String> tokens) {

        List<List<String>> stages = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String token : tokens) {

            if (token.equals("|")) {

                stages.add(current);
                current = new ArrayList<>();

            } else {

                current.add(token);
            }
        }

        stages.add(current);

        return stages;
    }

    private Path executePipeline(
            List<String> fullTokens,
            Path currentDirectory) throws Exception {

        List<String> tokens = new ArrayList<>(fullTokens);

        // Background pipelines aren't implemented. A trailing "&"
        // is stripped so it's never passed as a literal argument to
        // the last command; the pipeline simply always runs in the
        // foreground.
        if (!tokens.isEmpty()
                && tokens.get(tokens.size() - 1).equals("&")) {

            tokens.remove(tokens.size() - 1);
        }

        if (tokens.isEmpty()) {
            return currentDirectory;
        }

        List<List<String>> rawStages = splitIntoStages(tokens);

        int stageCount = rawStages.size();

        List<StageRedirection> redirections = new ArrayList<>();
        boolean[] isBuiltin = new boolean[stageCount];

        // =========================================================
        // VALIDATE BEFORE STARTING ANYTHING
        // =========================================================

        for (int i = 0; i < stageCount; i++) {

            List<String> rawStage = rawStages.get(i);

            StageRedirection redirection =
                    resolveStageRedirection(rawStage);

            if (redirection.cleanArgs.isEmpty()) {

                System.err.println(
                        "syntax error near unexpected token `|'"
                );

                return currentDirectory;
            }

            redirections.add(redirection);

            String name = redirection.cleanArgs.get(0);

            isBuiltin[i] = isDispatchableBuiltin(name);

            if (!isBuiltin[i]
                    && getCommandPath(name) == null) {

                PrintStream stageError =
                        openErrorStream(
                                redirection.stderrFile,
                                redirection.stderrAppend
                        );

                stageError.println(
                        name + ": command not found"
                );

                settleConsole(stageError);

                if (stageError != System.err) {
                    stageError.close();
                }

                return currentDirectory;
            }
        }

        // =========================================================
        // START / RUN EACH STAGE, WIRING ADJACENT STAGES TOGETHER
        // =========================================================

        Process[] processes = new Process[stageCount];

        // Either a Process (external stage) or a byte[] (captured
        // builtin output) - whatever the previous stage produced.
        Object previousSource = null;
        boolean previousWasBuiltin = false;

        for (int i = 0; i < stageCount; i++) {

            StageRedirection redirection = redirections.get(i);
            boolean isLast = (i == stageCount - 1);

            if (isBuiltin[i]) {

                ByteArrayOutputStream capture =
                        isLast ? null : new ByteArrayOutputStream();

                PrintStream stageOutput =
                        isLast
                                ? openOutputStream(
                                redirection.stdoutFile,
                                redirection.stdoutAppend)
                                : new PrintStream(capture);

                PrintStream stageError =
                        openErrorStream(
                                redirection.stderrFile,
                                redirection.stderrAppend
                        );

                if (i > 0) {

                    connectPreviousIntoBuiltin(
                            previousSource,
                            previousWasBuiltin
                    );
                }

                runBuiltin(
                        redirection.cleanArgs,
                        stageOutput,
                        stageError,
                        currentDirectory
                );

                closeRedirectedStreams(stageOutput, stageError);

                previousSource =
                        isLast ? null : capture.toByteArray();

                previousWasBuiltin = true;

            } else {

                ProcessBuilder processBuilder =
                        new ProcessBuilder(redirection.cleanArgs);

                // Run every pipeline stage from the shell's
                // current working directory.
                processBuilder.directory(
                        currentDirectory.toFile()
                );

                // ---------------------------------------------------
                // STDOUT
                //
                // Only the last stage's stdout can go anywhere but
                // the next pipe - either a redirected file or the
                // real terminal. Every other stage is left at the
                // ProcessBuilder default (PIPE) so its output can be
                // pumped into the next stage.
                // ---------------------------------------------------

                if (isLast) {

                    if (redirection.stdoutFile != null) {

                        File outFile =
                                new File(redirection.stdoutFile);

                        if (redirection.stdoutAppend) {

                            processBuilder.redirectOutput(
                                    ProcessBuilder.Redirect.appendTo(
                                            outFile
                                    )
                            );

                        } else {

                            processBuilder.redirectOutput(outFile);
                        }

                    } else {

                        processBuilder.redirectOutput(
                                ProcessBuilder.Redirect.INHERIT
                        );
                    }
                }

                // ---------------------------------------------------
                // STDERR - always the stage's own, independent of
                // its position in the pipe (errors are never piped
                // between stages, matching normal shell semantics
                // and the existing single-command behavior above).
                // ---------------------------------------------------

                if (redirection.stderrFile != null) {

                    File errFile =
                            new File(redirection.stderrFile);

                    if (redirection.stderrAppend) {

                        processBuilder.redirectError(
                                ProcessBuilder.Redirect.appendTo(
                                        errFile
                                )
                        );

                    } else {

                        processBuilder.redirectError(errFile);
                    }

                } else {

                    processBuilder.redirectError(
                            ProcessBuilder.Redirect.INHERIT
                    );
                }

                // Stage 0's stdin is deliberately left at the
                // ProcessBuilder default here, exactly matching how
                // a single external command is started above (this
                // class never calls redirectInput() there either).

                Process process = processBuilder.start();

                processes[i] = process;

                if (i > 0) {

                    OutputStream sink = process.getOutputStream();

                    if (previousWasBuiltin) {

                        feedStream((byte[]) previousSource, sink);

                    } else {

                        pumpStream(
                                ((Process) previousSource)
                                        .getInputStream(),
                                sink
                        );
                    }
                }

                previousSource = process;
                previousWasBuiltin = false;
            }
        }

        // =========================================================
        // PROCESS CLEANUP / WAITING
        // =========================================================

        for (int i = 0; i < stageCount - 1; i++) {

            if (processes[i] != null) {

                Process process = processes[i];

                Thread reaper = new Thread(() -> {

                    try {
                        process.waitFor();
                    } catch (InterruptedException ignored) {
                    }
                });

                reaper.setDaemon(true);
                reaper.start();
            }
        }

        Process lastProcess = processes[stageCount - 1];

        if (lastProcess != null) {
            lastProcess.waitFor();
        }

        return currentDirectory;
    }

    // =============================================================
    // BUILTIN DISPATCH FOR PIPELINE STAGES
    //
    // Mirrors the exact set of cases handled by the switch in
    // execute() above ("exit" is a recognized builtin name per
    // Builtins.NAMES but, exactly as in execute(), has no case
    // here either - it falls through to being treated as an
    // external command, unchanged from today's behavior).
    // =============================================================

    private boolean isDispatchableBuiltin(String name) {

        switch (name) {

            case "echo":
            case "type":
            case "pwd":
            case "cd":
            case "complete":
            case "jobs":
            case "history":
            case "declare":
            case "ai":
            case "project":
            case "tree":
                return true;

            default:
                return false;
        }
    }

    private void runBuiltin(
            List<String> args,
            PrintStream output,
            PrintStream errorOutput,
            Path currentDirectory) {

        switch (args.get(0)) {

            case "echo":

                new Quoting().executeEcho(args, output);
                return;

            case "type":

                executeType(args, output);
                return;

            case "pwd":

                output.println(currentDirectory);
                return;

            case "cd":

                if (args.size() < 2) {

                    errorOutput.println("cd: missing argument");
                    return;
                }

                // A "cd" inside a pipeline behaves like it does in
                // a real shell subshell: the target is validated
                // (and any error reported exactly as executeCd
                // already does) but the directory change never
                // escapes to the outer shell.
                executeCd(args.get(1), currentDirectory, errorOutput);

                return;

            case "complete":

                new CompleteBuiltin().execute(args, output, errorOutput);
                return;

            case "jobs":

                new JobsBuiltin().execute(output);
                return;

            case "history":

                new HistoryBuiltin().execute(args, output, errorOutput);
                return;

            case "declare":

                new DeclareBuiltin().execute(args, output, errorOutput);
                return;



            case "ai":

                new AIBuiltin(this).execute(
                        args,
                        output,
                        errorOutput,
                        currentDirectory,
                        reader
                );

                return;

            case "project":
                ProjectBuiltin.execute(currentDirectory);
                return;

            case "tree":
                TreeBuiltin.execute(currentDirectory, 3);
                return;


            default:
                // Unreachable: only names accepted by
                // isDispatchableBuiltin ever reach this method.
        }
    }

    // =============================================================
    // STREAM WIRING HELPERS
    // =============================================================

    private void connectPreviousIntoBuiltin(
            Object previousSource,
            boolean previousWasBuiltin) {

        // None of this shell's builtins ever read stdin. If the
        // previous stage was an external process, its stdout still
        // has to be drained so it never blocks writing into a full,
        // unread pipe - it's just discarded instead of being fed
        // anywhere. If the previous stage was a builtin, its
        // captured output is already fully in memory and needs no
        // draining at all.
        if (!previousWasBuiltin && previousSource != null) {
            drainStream(((Process) previousSource).getInputStream());
        }
    }

    private void pumpStream(InputStream source, OutputStream sink) {

        Thread pump = new Thread(() -> {

            byte[] buffer = new byte[8192];

            try {

                int bytesRead;

                while ((bytesRead = source.read(buffer)) != -1) {

                    sink.write(buffer, 0, bytesRead);
                    sink.flush();
                }

            } catch (IOException ignored) {
                // Downstream stage exited early (e.g. "head" after
                // enough lines) - stop pumping.
            } finally {

                try {
                    sink.close();
                } catch (IOException ignored) {
                }

                try {
                    // Closing our end of the upstream process's
                    // stdout causes it to receive SIGPIPE on its
                    // next write, so a producer like "tail -f"
                    // terminates naturally instead of running
                    // forever once nothing downstream needs it.
                    source.close();
                } catch (IOException ignored) {
                }
            }
        });

        pump.setDaemon(true);
        pump.start();
    }

    private void drainStream(InputStream source) {

        Thread drain = new Thread(() -> {

            byte[] buffer = new byte[8192];

            try {

                while (source.read(buffer) != -1) {
                    // Discarded - this stage's stdin is never read
                    // by any of the builtins, but the producing
                    // process must still not block on a full pipe.
                }

            } catch (IOException ignored) {
            } finally {

                try {
                    source.close();
                } catch (IOException ignored) {
                }
            }
        });

        drain.setDaemon(true);
        drain.start();
    }

    private void feedStream(byte[] data, OutputStream sink) {

        try {
            sink.write(data);
        } catch (IOException ignored) {
            // Downstream closed early; nothing more to feed.
        } finally {

            try {
                sink.close();
            } catch (IOException ignored) {
            }
        }
    }

    // =============================================================
    // PER-STAGE REDIRECTION PARSING
    //
    // Same operators, same scanning rule (leftmost redirect token
    // wins the cut point) as the single-command path above - just
    // returning the pieces instead of consuming them inline, so
    // each pipeline stage can be resolved independently.
    // =============================================================

    private static final class StageRedirection {

        final List<String> cleanArgs;
        final String stdoutFile;
        final boolean stdoutAppend;
        final String stderrFile;
        final boolean stderrAppend;

        StageRedirection(
                List<String> cleanArgs,
                String stdoutFile,
                boolean stdoutAppend,
                String stderrFile,
                boolean stderrAppend) {

            this.cleanArgs = cleanArgs;
            this.stdoutFile = stdoutFile;
            this.stdoutAppend = stdoutAppend;
            this.stderrFile = stderrFile;
            this.stderrAppend = stderrAppend;
        }
    }

    private StageRedirection resolveStageRedirection(
            List<String> tokens) {

        String stdoutFile = null;
        String stderrFile = null;

        int redirectIndex = -1;
        int errorRedirectIndex = -1;
        int appendIndex = -1;
        int errorAppendIndex = -1;

        for (int i = 0; i < tokens.size(); i++) {

            String token = tokens.get(i);

            if (token.equals(">") || token.equals("1>")) {

                redirectIndex = i;

                if (i + 1 < tokens.size()) {
                    stdoutFile = tokens.get(i + 1);
                }

            } else if (token.equals("2>")) {

                errorRedirectIndex = i;

                if (i + 1 < tokens.size()) {
                    stderrFile = tokens.get(i + 1);
                }

            } else if (token.equals(">>") || token.equals("1>>")) {

                appendIndex = i;

                if (i + 1 < tokens.size()) {
                    stdoutFile = tokens.get(i + 1);
                }

            } else if (token.equals("2>>")) {

                errorAppendIndex = i;

                if (i + 1 < tokens.size()) {
                    stderrFile = tokens.get(i + 1);
                }
            }
        }

        int firstRedirect = tokens.size();

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

        List<String> cleanArgs =
                new ArrayList<>(tokens.subList(0, firstRedirect));

        return new StageRedirection(
                cleanArgs,
                stdoutFile,
                appendIndex != -1,
                stderrFile,
                errorAppendIndex != -1
        );
    }

    private PrintStream openOutputStream(
            String file,
            boolean append) throws Exception {

        if (file == null) {
            return System.out;
        }

        File outFile = new File(file);

        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }

        return append
                ? new PrintStream(new FileOutputStream(outFile, true))
                : new PrintStream(outFile);
    }

    private PrintStream openErrorStream(
            String file,
            boolean append) throws Exception {

        if (file == null) {
            return System.err;
        }

        File errFile = new File(file);

        if (errFile.getParentFile() != null) {
            errFile.getParentFile().mkdirs();
        }

        return append
                ? new PrintStream(new FileOutputStream(errFile, true))
                : new PrintStream(errFile);
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

    public String getLastCommand() {
        return lastCommand;
    }

    public String getLastError() {
        return lastError;
    }
}
