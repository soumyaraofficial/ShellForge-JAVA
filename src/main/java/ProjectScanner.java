import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;

public class ProjectScanner {

    public static ProjectInfo scan(Path directory) {

        Map<String, Integer> languages = new HashMap<>();

        boolean git = Files.isDirectory(directory.resolve(".git"));
        boolean readme = Files.exists(directory.resolve("README.md"))
                || Files.exists(directory.resolve("README"));

        long totalFiles = 0;
        long sourceFiles = 0;
        List<String> technologies = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(directory)) {

            for (Path path : (Iterable<Path>) paths::iterator) {

                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String pathString = path.toString();

                // Ignore generated/dependency directories
                if (pathString.contains("/target/")
                        || pathString.contains("/node_modules/")
                        || pathString.contains("/.git/")
                        || pathString.contains("/build/")
                        || pathString.contains("/dist/")
                        || pathString.contains("/.idea/")) {
                    continue;
                }

                totalFiles++;

                String fileName = path.getFileName().toString();

                String language = detectLanguage(fileName);

                if (language != null) {
                    languages.merge(language, 1, Integer::sum);
                    sourceFiles++;
                }
            }

        } catch (IOException e) {
            System.err.println("Unable to scan project: " + e.getMessage());
        }

        String buildTool = detectBuildTool(directory);
        String primaryLanguage = detectPrimaryLanguage(languages);
        String projectType = detectProjectType(directory, primaryLanguage);
        detectTechnologies(directory, technologies);
        String sourceDirectory = detectDirectory(
                directory,
                "src",
                "app",
                "lib"
        );

        String testDirectory = detectDirectory(
                directory,
                "src/test",
                "tests",
                "test",
                "__tests__"
        );

        return new ProjectInfo(
                directory.getFileName() != null
                        ? directory.getFileName().toString()
                        : directory.toString(),
                primaryLanguage,
                projectType,
                buildTool,
                git,
                readme,
                sourceDirectory,
                testDirectory,
                totalFiles,
                sourceFiles,
                languages,
                technologies
        );
    }


    private static String detectLanguage(String fileName) {

        if (fileName.endsWith(".java")) {
            return "Java";
        }

        if (fileName.endsWith(".py")) {
            return "Python";
        }

        if (fileName.endsWith(".js")) {
            return "JavaScript";
        }

        if (fileName.endsWith(".jsx")) {
            return "JavaScript / React";
        }

        if (fileName.endsWith(".ts")) {
            return "TypeScript";
        }

        if (fileName.endsWith(".tsx")) {
            return "TypeScript / React";
        }

        if (fileName.endsWith(".c")) {
            return "C";
        }

        if (fileName.endsWith(".cpp")
                || fileName.endsWith(".cc")
                || fileName.endsWith(".cxx")) {
            return "C++";
        }

        if (fileName.endsWith(".go")) {
            return "Go";
        }

        if (fileName.endsWith(".rs")) {
            return "Rust";
        }

        if (fileName.endsWith(".kt")) {
            return "Kotlin";
        }

        if (fileName.endsWith(".swift")) {
            return "Swift";
        }

        if (fileName.endsWith(".rb")) {
            return "Ruby";
        }

        if (fileName.endsWith(".php")) {
            return "PHP";
        }

        if (fileName.endsWith(".sh")
                || fileName.endsWith(".bash")
                || fileName.endsWith(".zsh")) {
            return "Shell";
        }

        return null;
    }


    private static String detectBuildTool(Path directory) {

        if (Files.exists(directory.resolve("pom.xml"))) {
            return "Maven";
        }

        if (Files.exists(directory.resolve("build.gradle"))
                || Files.exists(directory.resolve("build.gradle.kts"))) {
            return "Gradle";
        }

        if (Files.exists(directory.resolve("package.json"))) {
            return "npm / Node.js";
        }

        if (Files.exists(directory.resolve("requirements.txt"))
                || Files.exists(directory.resolve("pyproject.toml"))
                || Files.exists(directory.resolve("setup.py"))) {
            return "pip / Python";
        }

        if (Files.exists(directory.resolve("Cargo.toml"))) {
            return "Cargo";
        }

        if (Files.exists(directory.resolve("go.mod"))) {
            return "Go";
        }

        if (Files.exists(directory.resolve("CMakeLists.txt"))) {
            return "CMake";
        }

        if (Files.exists(directory.resolve("composer.json"))) {
            return "Composer";
        }

        if (Files.exists(directory.resolve("Gemfile"))) {
            return "Bundler";
        }

        if (Files.exists(directory.resolve("Package.swift"))) {
            return "Swift Package Manager";
        }

        return "None detected";
    }


    private static String detectPrimaryLanguage(
            Map<String, Integer> languages) {

        if (languages.isEmpty()) {
            return "Unknown";
        }

        return languages.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }


    private static String detectProjectType(
            Path directory,
            String primaryLanguage) {

        if (Files.exists(directory.resolve("pom.xml"))) {
            return "Java Application";
        }

        if (Files.exists(directory.resolve("package.json"))) {

            if (Files.exists(directory.resolve("src"))
                    || Files.exists(directory.resolve("app"))) {
                return "JavaScript / Node Project";
            }

            return "Node.js Project";
        }

        if (Files.exists(directory.resolve("Cargo.toml"))) {
            return "Rust Project";
        }

        if (Files.exists(directory.resolve("go.mod"))) {
            return "Go Project";
        }

        if (Files.exists(directory.resolve("pyproject.toml"))
                || Files.exists(directory.resolve("requirements.txt"))) {
            return "Python Project";
        }

        if (Files.exists(directory.resolve("CMakeLists.txt"))) {
            return "C / C++ Project";
        }

        if (!primaryLanguage.equals("Unknown")) {
            return primaryLanguage + " Project";
        }

        return "Unknown Project";
    }
    private static void detectTechnologies(
            Path directory,
            List<String> technologies) {

        // ---------------------------------------------------------
        // Java / Maven
        // ---------------------------------------------------------

        Path pom = directory.resolve("pom.xml");

        if (Files.exists(pom)) {

            technologies.add("Maven");

            String content = readFile(pom);

            if (content.contains("org.jline")) {
                technologies.add("JLine");
            }

            if (content.contains("Java-WebSocket")) {
                technologies.add("Java-WebSocket");
            }

            if (content.contains("pty4j")) {
                technologies.add("pty4j");
            }

            if (content.contains("spring-boot")) {
                technologies.add("Spring Boot");
            }

            if (content.contains("hibernate")) {
                technologies.add("Hibernate");
            }

            if (content.contains("postgresql")) {
                technologies.add("PostgreSQL");
            }

            if (content.contains("mysql")) {
                technologies.add("MySQL");
            }
        }


        // ---------------------------------------------------------
        // JavaScript / React / Node
        // ---------------------------------------------------------

        Path packageJson =
                directory.resolve("package.json");

        if (Files.exists(packageJson)) {

            technologies.add("Node.js");
            technologies.add("npm");

            String content =
                    readFile(packageJson);

            if (content.contains("\"react\"")) {
                technologies.add("React");
            }

            if (content.contains("\"react-dom\"")) {
                technologies.add("React DOM");
            }

            if (content.contains("\"vite\"")) {
                technologies.add("Vite");
            }

            if (content.contains("\"next\"")) {
                technologies.add("Next.js");
            }

            if (content.contains("\"express\"")) {
                technologies.add("Express");
            }

            if (content.contains("\"typescript\"")) {
                technologies.add("TypeScript");
            }
        }
        // ---------------------------------------------------------
        // Python
        // ---------------------------------------------------------

        Path requirements =
                directory.resolve("requirements.txt");

        if (Files.exists(requirements)) {

            technologies.add("Python");

            String content =
                    readFile(requirements);

            if (content.toLowerCase().contains("django")) {
                technologies.add("Django");
            }

            if (content.toLowerCase().contains("flask")) {
                technologies.add("Flask");
            }

            if (content.toLowerCase().contains("fastapi")) {
                technologies.add("FastAPI");
            }

            if (content.toLowerCase().contains("numpy")) {
                technologies.add("NumPy");
            }

            if (content.toLowerCase().contains("pandas")) {
                technologies.add("Pandas");
            }
        }


        // ---------------------------------------------------------
        // Git
        // ---------------------------------------------------------

        if (Files.isDirectory(directory.resolve(".git"))) {
            technologies.add("Git");
        }


        // ---------------------------------------------------------
        // Remove duplicates
        // ---------------------------------------------------------

        List<String> unique =
                technologies.stream()
                        .distinct()
                        .toList();

        technologies.clear();
        technologies.addAll(unique);
    }

    private static String detectDirectory(
            Path directory,
            String... candidates) {

        for (String candidate : candidates) {

            if (Files.isDirectory(directory.resolve(candidate))) {
                return candidate;
            }
        }

        return "Not found";
    }


    private static String readFile(Path file) {

        try {

            return Files.readString(file);

        } catch (IOException e) {

            return "";
        }
    }
    public static class ProjectInfo {

        private final String name;
        private final String primaryLanguage;
        private final String projectType;
        private final String buildTool;
        private final boolean git;
        private final boolean readme;
        private final String sourceDirectory;
        private final String testDirectory;
        private final long totalFiles;
        private final long sourceFiles;
        private final Map<String, Integer> languages;
        private final List<String> technologies;

        public ProjectInfo(
                String name,
                String primaryLanguage,
                String projectType,
                String buildTool,
                boolean git,
                boolean readme,
                String sourceDirectory,
                String testDirectory,
                long totalFiles,
                long sourceFiles,
                Map<String, Integer> languages,
                List<String> technologies) {

            this.name = name;
            this.primaryLanguage = primaryLanguage;
            this.projectType = projectType;
            this.buildTool = buildTool;
            this.git = git;
            this.readme = readme;
            this.sourceDirectory = sourceDirectory;
            this.testDirectory = testDirectory;
            this.totalFiles = totalFiles;
            this.sourceFiles = sourceFiles;
            this.languages = languages;
            this.technologies = technologies;
        }


        public String getName() {
            return name;
        }

        public String getPrimaryLanguage() {
            return primaryLanguage;
        }
        public List<String> getTechnologies() {
            return technologies;
        }
        public String getProjectType() {
            return projectType;
        }


        public String getBuildTool() {
            return buildTool;
        }

        public boolean isGit() {
            return git;
        }

        public boolean hasReadme() {
            return readme;
        }

        public String getSourceDirectory() {
            return sourceDirectory;
        }

        public String getTestDirectory() {
            return testDirectory;
        }

        public long getTotalFiles() {
            return totalFiles;
        }

        public long getSourceFiles() {
            return sourceFiles;
        }

        public Map<String, Integer> getLanguages() {
            return languages;
        }
    }
}