package org.jenkinsci.plugins.buildwithparameters;

import hudson.Util;
import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.PasswordParameterValue;
import hudson.model.ParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TextParameterValue;
import java.util.List;

public class BuildParameter {

    static final String JOB_DEFAULT_PASSWORD_PLACEHOLDER = "job_default_password";
    private BuildParameterType type;
    private final String name;
    private final String description;
    private final String formattedDescription;
    private final ParameterDefinition parameterDefinition;
    private String value;
    private List<String> choices = null;

    public BuildParameter(String name, String description) {
        this(name, description, Util.escape(description), null);
    }

    BuildParameter(String name, String description, String formattedDescription) {
        this(name, description, formattedDescription, null);
    }

    BuildParameter(ParameterDefinition parameterDefinition) {
        this(parameterDefinition.getName(), parameterDefinition.getDescription(),
                parameterDefinition.getFormattedDescription(), parameterDefinition);
    }

    private BuildParameter(String name, String description, String formattedDescription,
            ParameterDefinition parameterDefinition) {
        this.name = name;
        this.description = description;
        this.formattedDescription = formattedDescription;
        this.parameterDefinition = parameterDefinition;
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

    public String getFormattedDescription() {
        return formattedDescription;
    }

    public ParameterDefinition getParameterDefinition() {
        return parameterDefinition;
    }

    public String getValue() {
        return value;
    }

    public void setValue(ParameterValue parameterValue) {
        if (parameterValue instanceof TextParameterValue) {
            this.value = ((TextParameterValue) parameterValue).getValue();
        } else if (parameterValue instanceof StringParameterValue) {
            this.value = ((StringParameterValue) parameterValue).getValue();
        } else if (parameterValue instanceof BooleanParameterValue) {
            this.value = String.valueOf(((BooleanParameterValue) parameterValue).getValue());
        } else if (parameterValue instanceof PasswordParameterValue) {
            this.value = JOB_DEFAULT_PASSWORD_PLACEHOLDER;
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
