# Repository guidance for coding agents

This is a Jenkins plugin that shows a confirmation form at a job's `/parambuild` URL before scheduling a parameterized build. Read [CONTRIBUTING.md](CONTRIBUTING.md) for development commands and [docs/architecture.md](docs/architecture.md) for the request flow.

## Code map

- `ParamBuilderActionFactory` attaches the action to parameterized jobs.
- `BuildWithParametersAction` reads parameter definitions and request values, handles form submission, and schedules the build.
- `BuildParameter` and `BuildParameterType` provide values for the Jelly form in `src/main/resources/org/jenkinsci/plugins/buildwithparameters/BuildWithParametersAction/index.jelly`.
- `src/test/java/org/jenkinsci/plugins/buildwithparameters/` contains unit and Jenkins integration tests.

## Change guidance

- Preserve the confirmation step: opening `/parambuild` must not schedule a build.
- Preserve the `BUILD` permission checks and `@RequirePOST` on submission. Keep Jelly output escaped and avoid exposing password values in the form.
- Let Jenkins parameter definitions create and validate submitted values. Keep the existing handling for unchecked booleans, absent parameter values, and the default password placeholder unless the task explicitly changes that behavior.
- Add or update a focused test when changing request handling, parameter rendering, permissions, or scheduling. Prefer `JenkinsRule` for behavior that crosses the HTTP form and build queue.
- Derive Java and Jenkins compatibility requirements from `pom.xml` and `Jenkinsfile`; do not copy version numbers into these instructions.
- Keep committed documentation free of personal information and machine-specific paths.
- Format commit messages as `<branch> - <commit type> - <commit message>`, using the current branch name and a type such as `feat`, `fix`, `docs`, `refactor`, `test`, or `chore`. Example: `master - docs - add contributor guidance`.
- Update the changelog in `README.md` for every pull request, adding the change under `Unreleased` until it is published.
- When creating a pull request for a bug fix, add the `bug` label.
- Before every push, ask for confirmation and wait for explicit approval, even if the original request includes pushing. Identify the remote and branch in the confirmation request.

## Verification

Use the commands in [CONTRIBUTING.md](CONTRIBUTING.md#build-and-test). Run `mvn test` for code changes, or `mvn package` to also produce the HPI. Documentation-only changes need checks for accuracy, local links, and whitespace. Report commands run and any failures; say when tests were not run.
