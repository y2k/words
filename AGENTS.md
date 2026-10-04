## Collaboration
- When asking the user a question during development, use OpenCode's `question` tool with answer options; set `multiple: true` only when more than one answer may apply.
- Implement tasks yourself in the current session by default. Do not ask the user to choose an execution method; delegate only when the user explicitly requests it.
- Work in the current checkout and on the current branch by default, including `main` or `master`. This is standing consent to work there without asking or creating a worktree. Use a different branch or worktree only when the user explicitly requests it. Do not overwrite, revert, or discard changes you did not make.
- Write plans in Russian.

## Python
- Use Python only for temporary scripts, and delete those scripts before completing the task. Do not add Python to persistent project code, tests, or build tooling.

## Compiler

The `l2yk` compiler source code is located in symlink `./compiler`.
Create issues for the `l2yk` compiler in https://github.com/y2k/language.

Third-party packages are located in the directory specified by the `LY2K_PACKAGES_DIR` environment variable.
