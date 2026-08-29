import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class JobManager {

    private JobManager() {
    }

    // =============================================================
    // JOB
    // =============================================================

    public static final class Job {

        public final int jobNumber;
        public final Process process;
        public final String command;

        private Job(
                int jobNumber,
                Process process,
                String command) {

            this.jobNumber = jobNumber;
            this.process = process;
            this.command = command;
        }
    }

    // =============================================================
    // Static so this survives across CommandExecutor invocations,
    // the same way CompletionRegistry backs `complete`.
    // =============================================================

    private static final List<Job> JOBS = new ArrayList<>();

    // =============================================================
    // START A NEW BACKGROUND JOB
    //
    // Job numbers recycle: the next number is always
    // (highest currently tracked job number) + 1, or 1 if the
    // table is empty - never a permanently incrementing counter.
    // =============================================================

    public static synchronized Job startJob(
            Process process,
            String command) {

        int jobNumber = nextJobNumber();

        Job job = new Job(jobNumber, process, command);

        JOBS.add(job);

        return job;
    }

    private static int nextJobNumber() {

        int max = 0;

        for (Job job : JOBS) {
            max = Math.max(max, job.jobNumber);
        }

        return max + 1;
    }

    // =============================================================
    // `jobs` BUILTIN OUTPUT
    //
    // Prints every currently tracked job (Running or freshly
    // completed) using markers computed from the FULL table as it
    // exists right now, then removes the completed ones. This is
    // why a job that just finished can appear as "Done" in the
    // same listing that still shows other jobs as "Running" - the
    // removal only happens after everything has been printed.
    // =============================================================

    public static synchronized void listJobs(PrintStream output) {

        for (String line : buildDisplayLines(true)) {
            output.println(line);
        }
    }

    // =============================================================
    // AUTOMATIC PRE-PROMPT REAPING
    //
    // Same detection/marker/removal logic as listJobs, but only
    // ever prints "Done" lines for jobs that just completed -
    // still-running jobs stay silent here.
    // =============================================================

    public static synchronized void reapBeforePrompt(PrintStream output) {

        for (String line : buildDisplayLines(false)) {
            output.println(line);
        }
    }

    // =============================================================
    // SHARED CORE
    //
    // Single method backing both listJobs and reapBeforePrompt so
    // detection, marker computation, and removal never drift apart.
    // =============================================================

    private static List<String> buildDisplayLines(boolean includeRunning) {

        List<String> lines = new ArrayList<>();

        if (JOBS.isEmpty()) {
            return lines;
        }

        List<Job> snapshot = sortedByJobNumber();

        Integer plusJobNumber = plusJobNumber(snapshot);
        Integer minusJobNumber = minusJobNumber(snapshot);

        List<Job> completed = new ArrayList<>();

        for (Job job : snapshot) {

            boolean alive = job.process.isAlive();

            String marker =
                    markerFor(
                            job.jobNumber,
                            plusJobNumber,
                            minusJobNumber
                    );

            if (!alive) {

                lines.add(
                        formatLine(
                                job.jobNumber,
                                marker,
                                "Done",
                                job.command
                        )
                );

                completed.add(job);

            } else if (includeRunning) {

                lines.add(
                        formatLine(
                                job.jobNumber,
                                marker,
                                "Running",
                                job.command + " &"
                        )
                );
            }
        }

        JOBS.removeAll(completed);

        return lines;
    }

    // =============================================================
    // MARKER HELPERS
    // =============================================================

    private static List<Job> sortedByJobNumber() {

        List<Job> copy = new ArrayList<>(JOBS);

        copy.sort(Comparator.comparingInt(job -> job.jobNumber));

        return copy;
    }

    private static Integer plusJobNumber(List<Job> sorted) {

        if (sorted.isEmpty()) {
            return null;
        }

        return sorted.get(sorted.size() - 1).jobNumber;
    }

    private static Integer minusJobNumber(List<Job> sorted) {

        if (sorted.size() < 2) {
            return null;
        }

        return sorted.get(sorted.size() - 2).jobNumber;
    }

    private static String markerFor(
            int jobNumber,
            Integer plusJobNumber,
            Integer minusJobNumber) {

        if (plusJobNumber != null && jobNumber == plusJobNumber) {
            return "+";
        }

        if (minusJobNumber != null && jobNumber == minusJobNumber) {
            return "-";
        }

        return " ";
    }

    // =============================================================
    // LINE FORMATTING
    //
    // "[N]" + marker + two literal spaces + status padded to a
    // 24-char field + command.
    // =============================================================

    private static String formatLine(
            int jobNumber,
            String marker,
            String status,
            String commandDisplay) {

        String statusField =
                String.format("%-24s", status);

        return "["
                + jobNumber
                + "]"
                + marker
                + "  "
                + statusField
                + commandDisplay;
    }
}