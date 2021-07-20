package org.jenkinsci.plugins.buildwithparameters;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.TextParameterValue;
import org.jenkinsci.plugins.buildwithparameters.definitions.CredentialParameterValue;

import java.util.List;

public class BuildParameter {

    static final String JOB_DEFAULT_PASSWORD_PLACEHOLDER = "job_default_password";
    static final String JOB_DEFAULT_CREDENTIAL_PLACEHOLDER = "credValue";
    private BuildParameterType type;
    private final String name;
    private final String description;
    private String value;
    private List<String> choices = null;

    public BuildParameter(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static boolean isDefaultPasswordPlaceholder(String candidate) {
        if (candidate == null) {
            return false;
        }
        return JOB_DEFAULT_PASSWORD_PLACEHOLDER.equals(candidate);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(ParameterValue parameterValue) {

        if (parameterValue instanceof StringParameterValue) {
            this.value = ((StringParameterValue) parameterValue).value;
        }

        else if (parameterValue instanceof TextParameterValue) {
            this.value = ((TextParameterValue) parameterValue).value;
        }

        else if (parameterValue instanceof BooleanParameterValue) {
            this.value = String.valueOf(((BooleanParameterValue) parameterValue).value);
        }

        else if (parameterValue instanceof PasswordParameterValue) {
            this.value = JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        }

        else if (parameterValue instanceof CredentialParameterValue) {
            this.value = ((CredentialParameterValue) parameterValue).value;
        }

        else {
            this.value = String.valueOf(parameterValue.getValue());
        }
    }

    public BuildParameterType getType() {
        return type;
    }

    public void setType(BuildParameterType type) {
        this.type = type;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

}
