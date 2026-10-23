# Contributing

## Set up

Install Maven and use one of the JDK versions listed in `Jenkinsfile`. Check `mvn --version` to confirm which Java installation Maven uses. The POM is the source of truth for the Jenkins baseline and plugin parent. Maven downloads Jenkins dependencies on the first build.

## Build and test

From the repository root:

```sh
mvn test
```

`mvn test` runs the JUnit and Jenkins test harness tests. To also create the HPI under `target/`, run `mvn package`, which includes the test phase. To run one test class during development:

```sh
mvn -Dtest=BuildWithParametersActionTest test
```

Start a local Jenkins with the plugin loaded using:

```sh
mvn hpi:run
```

Create a parameterized job, then append `/parambuild` to its job URL to try the confirmation form. For example, `/job/example/parambuild?message=hello` prefills a string parameter named `message`. Use the full job URL for jobs inside folders. Submitting the form requests a build through the Jenkins queue.

## Making changes

The request flow and security sensitive behavior are described in [docs/architecture.md](docs/architecture.md). Put Java changes in `src/main/java`, the form view in `src/main/resources`, and matching tests in `src/test/java`. For changes to form or submission behavior, test both the displayed values and the parameters received by the scheduled build.

Before opening a pull request, run `mvn test` and describe the behavior change and its tests. The Jenkinsfile defines the CI environments; use it to check the current JDK matrix.
