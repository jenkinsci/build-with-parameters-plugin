package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.PasswordParameterValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BuildParameterTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void setValue_passwordParam() {
        BuildParameter bp = new BuildParameter("n","v");
        bp.setValue(new PasswordParameterValue("asdf", "fdfd"));
        assertEquals(BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER, bp.getValue());
    }

    @Test
    void keepsTextAndChoiceValues() {
        BuildParameter parameter = new BuildParameter("message", "description", "formatted");
        parameter.setValue(new hudson.model.TextParameterValue("message", "line one\nline two"));
        assertEquals("line one\nline two", parameter.getValue());
        assertEquals("description", parameter.getDescription());
        assertEquals("formatted", parameter.getFormattedDescription());
        parameter.setChoices(java.util.List.of("first", "second"));
        assertEquals(java.util.List.of("first", "second"), parameter.getChoices());
    }

    @Test
    void isDefaultPasswordPlaceholder() {
        String expected = BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        assertFalse(BuildParameter.isDefaultPasswordPlaceholder(null));
        assertFalse(BuildParameter.isDefaultPasswordPlaceholder(""));
        assertFalse(BuildParameter.isDefaultPasswordPlaceholder(expected + "-"));
        assertTrue(BuildParameter.isDefaultPasswordPlaceholder(expected));
    }
}
