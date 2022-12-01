package org.jenkinsci.plugins.buildwithparameters;

import hudson.model.Action;
import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.TextParameterDefinition;
import hudson.util.Secret;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class BuildWithParametersAction<T extends Job<?, ?> & ParameterizedJob> implements Action {
    private static final String URL_NAME = "parambuild";

    private final T project;

    public BuildWithParametersAction(T project) {
        this.project = project;
    }

    //////////////////
    //              //
    //     VIEW     //
    //              //
    //////////////////
    public String getProjectName() {
        return project.getName();
    }

    public List<BuildParameter> getAvailableParameters() {
        List<BuildParameter> buildParameters = new ArrayList<>();

        for (ParameterDefinition parameterDefinition : getParameterDefinitions()) {
            BuildParameter buildParameter = new BuildParameter(parameterDefinition.getName(), parameterDefinition.getDescription());
            if (parameterDefinition.getClass().isAssignableFrom(PasswordParameterDefinition.class)) {
                buildParameter.setType(BuildParameterType.PASSWORD);
            } else if (parameterDefinition.getClass().isAssignableFrom(BooleanParameterDefinition.class)) {
                buildParameter.setType(BuildParameterType.BOOLEAN);
            } else if (parameterDefinition.getClass().isAssignableFrom(ChoiceParameterDefinition.class)) {
                buildParameter.setType(BuildParameterType.CHOICE);
                buildParameter.setChoices(((ChoiceParameterDefinition) parameterDefinition).getChoices());
            } else if (parameterDefinition.getClass().isAssignableFrom(StringParameterDefinition.class)) {
                buildParameter.setType(BuildParameterType.STRING);
            } else if (parameterDefinition.getClass().isAssignableFrom(TextParameterDefinition.class)) {
                buildParameter.setType(BuildParameterType.TEXT);
            } else {
                // default to string
                buildParameter.setType(BuildParameterType.STRING);
            }

            try {
                buildParameter.setValue(getParameterDefinitionValue(parameterDefinition));
            } catch (IllegalArgumentException ignored) {
                // If a value was provided that does not match available options, leave the value blank.
            }

            buildParameters.add(buildParameter);
        }

        return buildParameters;
    }

    ParameterValue getParameterDefinitionValue(ParameterDefinition parameterDefinition) {
        return parameterDefinition.createValue(Stapler.getCurrentRequest());
    }

    public String getIconFileName() {
        return null; // Invisible
    }

    public String getDisplayName() {
        return null; // Invisible
    }

    public String getUrlName() {
        project.checkPermission(BuildableItem.BUILD);
        return URL_NAME;
    }

    //////////////////
    //              //
    //  SUBMISSION  //
    //              //
    //////////////////
    @RequirePOST
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        project.checkPermission(BuildableItem.BUILD);

        List<ParameterValue> values = new ArrayList<>();

        JSONObject formData = req.getSubmittedForm();
        if (!formData.isEmpty()) {
            for (ParameterDefinition parameterDefinition : getParameterDefinitions()) {
                ParameterValue parameterValue = parameterDefinition.createValue(req);
                if (parameterValue != null) {
                    if (parameterValue.getClass().isAssignableFrom(BooleanParameterValue.class)) {
                        boolean value = (req.getParameter(parameterDefinition.getName()) != null);
                        parameterValue = ((BooleanParameterDefinition) parameterDefinition).createValue(String.valueOf(value));
                    } else if (parameterValue.getClass().isAssignableFrom(PasswordParameterValue.class)) {
                        parameterValue = applyDefaultPassword((PasswordParameterDefinition) parameterDefinition,
                                                                (PasswordParameterValue) parameterValue);
                    }
                }
                // This will throw an exception if the provided value is not a valid option for the parameter.
                // This is the desired behavior, as we want to ensure valid submissions.
                values.add(parameterValue);
            }
        }

        Jenkins.get().getQueue().schedule(project, 0, new ParametersAction(values), new CauseAction(new Cause.UserIdCause()));
        rsp.sendRedirect("../");
    }

    ParameterValue applyDefaultPassword(PasswordParameterDefinition parameterDefinition,
            PasswordParameterValue parameterValue) {
        String jobPassword = getPasswordValue(parameterValue);
        if (!BuildParameter.isDefaultPasswordPlaceholder(jobPassword)) {
            return parameterValue;
        }
        PasswordParameterValue password = (PasswordParameterValue) parameterDefinition.getDefaultParameterValue();
        String jobDefaultPassword = password != null ? getPasswordValue(password) : "";
        return new PasswordParameterValue(parameterValue.getName(), jobDefaultPassword);
    }

    static String getPasswordValue(PasswordParameterValue parameterValue) {
        Secret secret = parameterValue.getValue();
        return Secret.toString(secret);
    }

    //////////////////
    //              //
    //   HELPERS    //
    //              //
    //////////////////
    private List<ParameterDefinition> getParameterDefinitions() {
        ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);
        if (property != null && property.getParameterDefinitions() != null) {
            return property.getParameterDefinitions();
        }
        return new ArrayList<>();
    }

}
