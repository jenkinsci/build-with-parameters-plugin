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
import jakarta.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
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
            BuildParameter buildParameter = new BuildParameter(parameterDefinition);
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
                buildParameter.setType(BuildParameterType.CUSTOM);
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
        return parameterDefinition.createValue(Stapler.getCurrentRequest2());
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
    public void doConfigSubmit(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        project.checkPermission(BuildableItem.BUILD);

        List<ParameterValue> values = new ArrayList<>();

        JSONObject formData = req.getSubmittedForm();
        if (!formData.isEmpty()) {
            for (ParameterDefinition parameterDefinition : getParameterDefinitions()) {
                JSONObject parameterData = findParameterData(formData, parameterDefinition.getName());
                // File parameter forms include a file token even when no file was selected.
                // Keep Jenkins' request-based handling for that case, which omits the value.
                boolean missingFileValue = parameterDefinition instanceof hudson.model.FileParameterDefinition
                        && parameterData != null
                        && !parameterData.has("value");
                ParameterValue parameterValue = parameterData != null && !missingFileValue
                        ? parameterDefinition.createValue(req, parameterData)
                        : parameterDefinition.createValue(req);
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
                if (parameterValue != null) {
                    values.add(parameterValue);
                }
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

    private JSONObject findParameterData(JSONObject formData, String parameterName) {
        Object parameters = formData.opt("parameter");
        if (parameters instanceof JSONObject parameterData) {
            return parameterName.equals(parameterData.optString("name")) ? parameterData : null;
        }
        if (parameters instanceof JSONArray parameterArray) {
            for (Object parameter : parameterArray) {
                if (parameter instanceof JSONObject parameterData
                        && parameterName.equals(parameterData.optString("name"))) {
                    return parameterData;
                }
            }
        }
        return null;
    }

}
