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

## Features

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