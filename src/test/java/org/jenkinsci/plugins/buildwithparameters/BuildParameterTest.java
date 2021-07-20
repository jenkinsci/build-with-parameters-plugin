package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.model.PasswordParameterValue;

import org.jenkinsci.plugins.buildwithparameters.definitions.CredentialParameterValue;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BuildParameterTest {
    @Rule public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void setValue_passwordParam() {
        BuildParameter bp = new BuildParameter("n","v");
        bp.setValue(new PasswordParameterValue("asdf", "fdfd"));
        assertEquals(BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER, bp.getValue());
    }

    @Test
    public void setValue_credentialParam() {
        BuildParameter bp = new BuildParameter("n","v");
        bp.setValue(new CredentialParameterValue("cred", "credValue"));
        assertEquals(BuildParameter.JOB_DEFAULT_CREDENTIAL_PLACEHOLDER, bp.getValue());
    }
    
    @Test
    public void isDefaultPasswordPlaceholder() {
        String expected = BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        assertFalse(BuildParameter.isDefaultPasswordPlaceholder(null));
        assertFalse(BuildParameter.isDefaultPasswordPlaceholder(""));
        assertFalse(BuildParameter.isDefaultPasswordPlaceholder(expected + "-"));
        assertTrue(BuildParameter.isDefaultPasswordPlaceholder(expected));
    }
}
