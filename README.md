Build with Parameters Plugin
============================

Allows the user to provide parameters for a build in the url, prompting for confirmation before triggering the job.

The plugin exposes `$JENKINS/job/$JOB/parambuild` url to trigger the build with parameter.
Unlike `$JENKINS/job/$JOB/buildWithParameters`, a human must confirm that the parameters are correct before the build is triggered.
This is useful if you want to create a list of jobs with parameters to trigger ahead of time, and execute it at some future date (e.g. a deployment plan).

An example triggering of a job with the plugin:  
![](example_screenshot.png)


## Build
Running `mvn hpi:run` will get you hpi plugin file in "./target" directory

## Changelog

The latest releases are documented in the [GitHub releases page](https://github.com/jenkinsci/build-with-parameters-plugin/releases).

### 1.7 - Mar 5, 2022

- Update to support icons removal from Jenkins Core
- Bumped parent POM and minimal Jenkins version are now 2.204

### 1.6 - Oct 13, 2021

- Move documentation to GitHub

### 1.5.1 - Mar 26, 2021

- Fix XXS vulnerability as reported in [security advisory](https://www.jenkins.io/security/advisory/2021-03-30/#SECURITY-2231).
- Fix CRSF vulnerability as reported in [security advisory](https://www.jenkins.io/security/advisory/2021-03-30/#SECURITY-2257).

### 1.5 - Oct 4, 2020

- Require Jenkins 2.60.3 or newer.

### 1.4 - Jun 2, 2017

-   Fixed 404 error for not logged user
    ([JENKINS-25534](https://issues.jenkins-ci.org/browse/JENKINS-25534)).

-   Support for pipelines
    ([JENKINS-35378](https://issues.jenkins-ci.org/browse/JENKINS-35378)).

### 1.3 - Nov 10, 2014

-   Hide job action from UI to avoid confusion
    ([JENKINS-25413](https://issues.jenkins-ci.org/browse/JENKINS-25413)).
    
