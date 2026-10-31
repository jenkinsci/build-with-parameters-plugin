# Request flow

The plugin provides `/job/JOB_NAME/parambuild` for parameterized Jenkins jobs. A person reviews a form before the job is scheduled. The regular Jenkins `buildWithParameters` endpoint has a different interaction: this plugin's URL first displays a confirmation page.

1. `ParamBuilderActionFactory` creates a `BuildWithParametersAction` for jobs implementing `ParameterizedJob`. The action's URL name is `parambuild`; its icon and display name are `null`, so it does not add a visible job action link.
2. When the page is requested, `BuildWithParametersAction.getAvailableParameters()` reads the job's `ParametersDefinitionProperty`. Each definition becomes a `BuildParameter` for the Jelly view. `ParameterDefinition.createValue` reads the current request, allowing URL query values to prefill the form. If it throws `IllegalArgumentException`, the model value remains unset; the displayed result depends on the control.
3. `src/main/resources/org/jenkinsci/plugins/buildwithparameters/BuildWithParametersAction/index.jelly` renders controls for password, boolean, choice, string, and text parameters. Other definition types use the string control. The view escapes output by default. Parameter names are explicitly escaped; descriptions use `ParameterDefinition.getFormattedDescription()` to honor Jenkins' configured markup formatter, matching the standard build form.
4. The form posts to `configSubmit`, handled by `doConfigSubmit`. The method requires POST and checks `BuildableItem.BUILD` permission. For a nonempty submitted form, Jenkins parameter definitions create the `ParameterValue` objects. The method handles unchecked booleans, skips null values, and resolves the password placeholder to the job's default password.
5. The action requests scheduling through the Jenkins queue with a `ParametersAction` and `Cause.UserIdCause`, then redirects to the job page. It does not check whether the queue accepted the request.

## Behavior to protect

- The GET request only prepares the confirmation form; scheduling happens on the POST path.
- Both the action URL and submission check `BUILD` permission. Submission is guarded by `@RequirePOST`.
- When `createValue` returns a `PasswordParameterValue`, the form model substitutes `job_default_password` for the secret. On submission, that exact placeholder restores the job's configured default, or an empty string if there is no default.
- Parameter validation belongs to each Jenkins `ParameterDefinition`. Invalid submitted choices should fail rather than silently reach the queue.

`BuildWithParametersActionTest` exercises form submission with the Jenkins test harness. `BuildParameterTest` covers the password placeholder model behavior.
