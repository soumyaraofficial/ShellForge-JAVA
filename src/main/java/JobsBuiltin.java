import java.io.PrintStream;

public class JobsBuiltin {

    // =============================================================
    // EXECUTE
    //
    // Delegates entirely to JobManager, which owns all job state
    // and the shared reap/format/marker logic.
    // =============================================================

    public void execute(PrintStream output) {

        JobManager.listJobs(output);
    }
}