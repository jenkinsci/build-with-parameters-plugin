package org.jenkinsci.plugins.buildwithparameters.definitions;

import hudson.model.ParameterValue;
import hudson.model.PasswordParameterValue;
import hudson.model.SimpleParameterDefinition;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;

public class CredentialParameterDefinition extends SimpleParameterDefinition {
    protected CredentialParameterDefinition(String name) {
        super(name);
    }

    protected CredentialParameterDefinition(String name, String description) {
        super(name, description);
    }

    @Override
    public CredentialParameterValue createValue(String value) {
        return new CredentialParameterValue(getName(), getDescription());
    }

    @CheckForNull
    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return null;
    }
}
