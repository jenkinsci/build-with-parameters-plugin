package org.jenkinsci.plugins.buildwithparameters.definitions;

import hudson.model.ParameterValue;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.CheckForNull;

public class CredentialParameterValue extends ParameterValue {

    public String value;

    public CredentialParameterValue(String name, String value) {
        this(name, value, null);
    }

    @DataBoundConstructor
    public CredentialParameterValue(String name, String value, String description) {
        super(name, description);
        this.value = value;
    }

    @CheckForNull
    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
