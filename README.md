# ShellForge-JAVA

A Java-based Unix-like developer shell with an interactive terminal experience, AI-assisted commands, process management, developer-focused project inspection tools, and a React-based web terminal.

---

## Overview

**ShellForge-JAVA** is a custom shell built in Java that combines traditional Unix shell functionality with developer-oriented tooling and a local AI assistant.

The project started as a command-line shell implementation and evolved into a developer-focused terminal environment supporting:

- Command parsing and quoting
- Pipelines and redirection
- Background processes and job management
- Command history and tab completion
- AI-assisted command generation and explanation
- Project structure and technology analysis
- Recursive source-code search
- File and directory inspection
- A browser-based terminal using React and xterm.js
- PTY-based communication between the browser and Java shell

The AI functionality runs locally through **Ollama**, allowing natural-language interaction without requiring a cloud AI API.

---

## Key Features

### Shell Engine

ShellForge provides core shell functionality including:

- External command execution
- Built-in commands
- Command parsing and quoting
- Environment variable expansion
- Pipelines
- Standard output redirection
- Standard error redirection
- Append redirection
- Background processes
- Job management
- Command history
- Command completion
- File-name completion

Example:

```bash
$ echo "Hello ShellForge"
Hello ShellForge

$ echo "Hello" > hello.txt

$ cat hello.txt
Hello
```

#screenshot
Suggested screenshot: ShellForge running basic shell commands such as `pwd`, `echo`, `ls`, redirection, and a pipeline.

---

### Developer Tools

ShellForge includes custom commands designed specifically for working with software projects.

#### `project`

Analyzes the current project and provides developer-focused information such as:

- Programming languages
- Build tools
- Project structure
- Git information
- Source directories
- Test directories
- File statistics
- Detected technologies and frameworks

Example:

```bash
$ project
```

#screenshot
Suggested screenshot: The `project` command showing the project's languages, build system, Git information, source/test directories, file statistics, and detected technologies.

---

#### `tree`

Displays the directory structure with configurable depth.

```bash
$ tree
```

```bash
$ tree 2
```

```bash
$ tree src/main/java 2
```

Generated directories such as `.git`, `target`, `node_modules`, `build`, `dist`, and `.idea` are ignored.

#screenshot
Suggested screenshot: `tree src/main/java 2` showing the Java project structure.

---

#### `findx`

Performs recursive text searches across source and project files.

```bash
$ findx import
```

Example output:

```text
src/main/java/Shell.java
  1: import java.io.IOException;
  2: import java.nio.file.Path;

src/main/java/CommandExecutor.java
  1: import java.io.PrintStream;

────────────────────────────
12 matches in 4 files
```

`findx` focuses on common source and project file formats while ignoring generated directories and common dependency lock files.

#screenshot
Suggested screenshot: `findx import` showing grouped file results and matching line numbers.

---

#### `inspect`

Provides information about files and directories.

For a file:

```bash
$ inspect src/main/java/AIBuiltin.java
```

Example information includes:

```text
Inspect
────────────────────────────
Path       : src/main/java/AIBuiltin.java
Type       : Java Source File
Size       : 18.97 KB
Lines      : 773
Modified   : ...
────────────────────────────
```

For a directory:

```bash
$ inspect src/main/java
```

The directory inspection reports:

- Number of files
- Number of directories
- Total size
- File-type distribution
- Last modified time

#screenshot
Suggested screenshot: `inspect src/main/java` showing file count, directory count, total size, and file-type distribution.

---

## AI Assistant

ShellForge integrates with **Ollama** to provide a local AI assistant directly inside the shell.

The current implementation uses:

```text
qwen2.5:3b
```

The AI assistant can:

- Generate shell commands
- Explain commands
- Execute AI-generated commands with confirmation
- Help diagnose shell errors

### Generate a Command

```bash
$ ai create a file named hello.txt
```

The AI returns a suggested shell command instead of immediately executing it.

#screenshot
Suggested screenshot: `ai create a file named hello.txt` showing the generated command.

---

### Execute an AI-Generated Command

```bash
$ ai execute create a file named hello.txt
```

ShellForge generates the command and asks for confirmation before execution.

This provides an additional safety layer instead of blindly executing AI-generated commands.

#screenshot
Suggested screenshot: `ai execute ...` showing the generated command and confirmation prompt.

---

### Explain a Command

```bash
$ ai explain "find . -name '*.java'"
```

The AI explains what the command does and how its individual parts work.

#screenshot
Suggested screenshot: `ai explain ...` showing the AI explanation.

---

### Fix the Last Failed Command

```bash
$ ai fix
```

ShellForge uses information from the previous shell command and its captured error to request a possible correction from the local AI model.

#screenshot
Suggested screenshot: A failed command followed by `ai fix` and the suggested correction.

---

## Web Terminal

ShellForge also includes a browser-based terminal interface.

The web terminal is built using:

- React
- xterm.js
- WebSocket
- Java WebSocket
- PTY4J

The architecture allows the browser terminal to communicate with a real Java shell process through a pseudo-terminal.

```text
Browser
   │
   │ WebSocket
   ▼
WebServer
   │
   │ PTY
   ▼
ShellForge-JAVA
   │
   ├── Shell
   ├── CommandExecutor
   ├── Builtins
   ├── JobManager
   └── AI Assistant
```

The frontend provides a terminal-style interface while the Java backend handles the actual shell execution.

#screenshot
Suggested screenshot: Main retro-style ShellForge web terminal interface.

#screenshot
Suggested screenshot: A closer view of the terminal area showing the retro CRT/grain/glitch styling.

---

## Built-in Commands

| Command | Description |
|---|---|
| `echo` | Print text |
| `cd` | Change directory |
| `pwd` | Print current directory |
| `type` | Identify a command |
| `history` | Display command history |
| `jobs` | Display background jobs |
| `declare` | Manage shell variables |
| `complete` | Configure command completion |
| `ai` | Interact with the local AI assistant |
| `project` | Analyze the current project |
| `tree` | Display directory structure |
| `findx` | Search project files recursively |
| `inspect` | Inspect files and directories |
| `exit` | Exit the shell |

---

## Example Usage

### Basic Shell Commands

```bash
$ pwd

$ cd src

$ ls

$ echo "Hello World"
```

### Redirection

```bash
$ echo "Hello ShellForge" > hello.txt
```

Append output:

```bash
$ echo "Another line" >> hello.txt
```

### Pipelines

```bash
$ cat hello.txt | grep Hello
```

### Background Processes

```bash
$ sleep 10 &
```

View active jobs:

```bash
$ jobs
```

### Project Analysis

```bash
$ project
```

### Directory Structure

```bash
$ tree src/main 2
```

### Source Search

```bash
$ findx import
```

### File Inspection

```bash
$ inspect src/main/java/AIBuiltin.java
```

### AI

```bash
$ ai create a directory named demo
```

```bash
$ ai explain "find . -name '*.java'"
```

```bash
$ ai execute create a file named test.txt
```

#screenshot
Suggested screenshot: A combined terminal session showing several of the commands above in sequence.

---

## Architecture

ShellForge is divided into several major components.

### 1. Shell Layer

Responsible for:

- Terminal initialization
- Command input
- History
- Completion
- Prompt handling
- Shell lifecycle

Implemented primarily through **JLine**.

### 2. Command Execution Layer

`CommandExecutor` handles:

- Command parsing
- Parameter expansion
- Pipelines
- Redirection
- Built-in command dispatch
- External process execution
- Background process handling

### 3. Built-in Command Layer

Custom functionality is implemented through separate built-in command classes such as:

```text
AIBuiltin
ProjectBuiltin
TreeBuiltin
FindxBuiltin
InspectBuiltin
JobsBuiltin
```

### 4. Project Analysis Layer

`ProjectScanner` analyzes a project directory and detects information such as:

- Languages
- Build systems
- Git usage
- Source/test directories
- File statistics
- Technologies

### 5. AI Layer

`AIBuiltin` communicates with a local Ollama server and uses the generated response to assist with shell commands.

### 6. Web Terminal Layer

`WebServer` creates a pseudo-terminal using PTY4J and exposes shell communication through WebSocket connections.

The React frontend connects to this WebSocket and renders the shell through xterm.js.

---

## Architecture Flow

```text
                         ┌───────────────────────┐
                         │   React Web Frontend  │
                         │      xterm.js         │
                         └───────────┬───────────┘
                                     │
                                  WebSocket
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │      WebServer        │
                         │   Java-WebSocket      │
                         └───────────┬───────────┘
                                     │
                                   PTY4J
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │     ShellForge        │
                         │                       │
                         │       Shell           │
                         │         │             │
                         │  CommandExecutor      │
                         │         │             │
                         │      Builtins         │
                         └───────┬───────┬───────┘
                                 │       │
                                 │       │
                                 ▼       ▼
                         Developer     AI Assistant
                            Tools          │
                                           │
                                           ▼
                                      ┌──────────┐
                                      │  Ollama  │
                                      │ Qwen 2.5 │
                                      │   3B     │
                                      └──────────┘
```

#screenshot
Suggested screenshot: Architecture diagram or a visual overview of the complete ShellForge system.

---

## Project Structure

```text
ShellForge-JAVA/
│
├── src/
│   └── main/
│       └── java/
│           ├── Main.java
│           ├── Shell.java
│           ├── CommandExecutor.java
│           ├── CommandCompletion.java
│           ├── FileNameCompleter.java
│           ├── Builtins.java
│           ├── JobManager.java
│           │
│           ├── AIBuiltin.java
│           ├── ProjectBuiltin.java
│           ├── ProjectScanner.java
│           ├── TreeBuiltin.java
│           ├── FindxBuiltin.java
│           ├── InspectBuiltin.java
│           │
│           └── WebServer.java
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── pom.xml
├── .gitignore
└── README.md
```

---

## Tech Stack

### Backend

| Technology | Purpose |
|---|---|
| Java 26 | Core shell implementation |
| Maven | Build and dependency management |
| JLine | Interactive terminal, history and completion |
| Java-WebSocket | WebSocket server |
| PTY4J | Pseudo-terminal process management |
| Ollama | Local AI inference |

### Frontend

| Technology | Purpose |
|---|---|
| React | Web terminal UI |
| xterm.js | Terminal rendering |
| xterm.js Fit Addon | Terminal sizing |
| Vite | Frontend development and build tooling |

### AI

```text
ShellForge
     │
     ▼
  Ollama
     │
     ▼
Qwen 2.5 3B
```

The AI assistant runs locally through Ollama.

---

## Requirements

Before running ShellForge, install:

- Java JDK 26
- Maven
- Node.js and npm
- Ollama — only required for AI functionality

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

Verify Node.js:

```bash
node -v
```

Verify npm:

```bash
npm -v
```

---

## Installation

Clone the repository:

```bash
git clone https://github.com/soumyaraofficial/ShellForge-JAVA.git
```

Enter the project directory:

```bash
cd ShellForge-JAVA
```

Build the project:

```bash
mvn clean package
```

The executable JAR is generated under:

```text
target/shellforge-java.jar
```

---

## Running the Shell

Run the packaged shell:

```bash
java -jar target/shellforge-java.jar
```

You should see the ShellForge prompt:

```text
$
```

You can now use standard shell functionality and ShellForge's custom developer commands.

#screenshot
Suggested screenshot: ShellForge starting successfully and displaying the `$` prompt.

---

## Running the AI Assistant

Install Ollama and make sure the local Ollama service is running.

Pull the model:

```bash
ollama pull qwen2.5:3b
```

Then start ShellForge:

```bash
java -jar target/shellforge-java.jar
```

Example:

```bash
$ ai create a file called hello.txt
```

---

## Running the Web Terminal

First build the Java backend:

```bash
mvn clean package
```

Then install the frontend dependencies:

```bash
cd frontend
npm install
```

Start the frontend development server:

```bash
npm run dev
```

In another terminal, start the Java WebSocket/PTY server from the project root:

```bash
java -cp target/shellforge-java.jar WebServer
```

The WebServer listens on:

```text
ws://localhost:8080
```

The React development server can then connect to the Java shell through the WebSocket connection.

#screenshot
Suggested screenshot: The running browser-based ShellForge terminal after connecting to the Java backend.

---

## Security Considerations

AI-generated shell commands can potentially perform destructive operations.

ShellForge therefore separates command suggestion from execution.

For example:

```bash
$ ai create a file named hello.txt
```

generates a command without automatically executing it.

Where execution is explicitly requested:

```bash
$ ai execute create a file named hello.txt
```

the generated command is displayed and requires user confirmation before execution.

The AI assistant also runs locally through Ollama rather than sending shell requests to a remote AI service.

Users should still review AI-generated commands before approving them.

---

## Screenshots

#screenshot
Main ShellForge terminal interface.

#screenshot
Retro CRT-style web terminal.

#screenshot
AI command generation.

#screenshot
AI command execution with confirmation.

#screenshot
Project analysis using `project`.

#screenshot
Directory structure using `tree`.

#screenshot
Source search using `findx`.

#screenshot
File/directory inspection using `inspect`.

---

## Future Improvements

Possible future improvements include:

- More comprehensive automated testing
- More detailed source-code inspection
- Improved error capture for AI-assisted debugging
- Additional shell compatibility
- Improved terminal session management
- Release packaging and distribution

---

## Author

**Soumya Ranjan Panda**

Java / Backend Developer

GitHub: 
https://github.com/soumyaraofficial
