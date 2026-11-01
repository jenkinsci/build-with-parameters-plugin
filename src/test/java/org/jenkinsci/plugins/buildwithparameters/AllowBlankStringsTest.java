package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AllowBlankStringsTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void defaultsToAllowingBlankStrings() throws Exception {
        FreeStyleProject project = projectWithStringParameter("");

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage page = wc.getPage(project, "parambuild");
            assertTrue(page.getFormByName("config").getActionAttribute().endsWith("configSubmit"));
            valueInput(page).setValue("");
            submit(page);
        }

        j.waitUntilNoActivity();
        assertNotNull(project.getLastBuild());
        ParametersAction parameters = project.getLastBuild().getAction(ParametersAction.class);
        assertEquals("", ((StringParameterValue) parameters.getParameter("branch")).getValue());
    }

    @Test
    void rejectsBlankStringsWhenDisabledByUrlParameter() throws Exception {
        FreeStyleProject project = projectWithStringParameter("");

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage page = wc.getPage(project, "parambuild?allowBlankStrings=false");
            assertTrue(page.getFormByName("config").getActionAttribute().contains("__bwAllowBlankStrings=false"));
            valueInput(page).setValue("  \t ");
            HtmlPage response = submit(page);
            assertRejected(response);
        }

        assertNull(project.getLastBuild());
        assertNull(j.jenkins.getQueue().getItem(project));
    }

    @Test
    void rejectsMissingStringsWhenDisabledAndDefaultIsBlank() throws Exception {
        FreeStyleProject project = projectWithStringParameter("");

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage page = wc.getPage(project, "parambuild?allowBlankStrings=false");
            valueInput(page).getParentNode().remove();
            HtmlPage response = submit(page);
            assertRejected(response);
        }

        assertNull(project.getLastBuild());
        assertNull(j.jenkins.getQueue().getItem(project));
    }

    private FreeStyleProject projectWithStringParameter(String defaultValue) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("branch", defaultValue)));
        return project;
    }

    private HtmlInput valueInput(HtmlPage page) {
        return (HtmlInput) page.getByXPath("//input[@name='branch']").get(0);
    }

    private HtmlPage submit(HtmlPage page) throws Exception {
        HtmlForm form = page.getFormByName("config");
        DomElement submit = page.createElement("button");
        submit.setAttribute("type", "submit");
        form.appendChild(submit);
        return submit.click();
    }

    private void assertRejected(HtmlPage response) {
        int status = response.getWebResponse().getStatusCode();
        assertEquals(400, status);
    }
}
