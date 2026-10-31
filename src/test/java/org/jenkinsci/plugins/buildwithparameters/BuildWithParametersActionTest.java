package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Util;
import hudson.markup.MarkupFormatter;
import hudson.markup.EscapedMarkupFormatter;
import hudson.model.FileParameterDefinition;
import hudson.model.FreeStyleProject;
import hudson.plugins.validating_string_parameter.ValidatingStringParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.Queue;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterDefinition;
import org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterValue;
import org.jvnet.jenkins.plugins.nodelabelparameter.NodeParameterDefinition;
import org.jvnet.jenkins.plugins.nodelabelparameter.NodeParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.Writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BuildWithParametersActionTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void uploadsSingleFileParameter() throws Exception {
        verifyFileUpload(false, "uploaded content");
    }

    @Test
    void uploadsFileAlongsideOtherParameters() throws Exception {
        verifyFileUpload(true, "uploaded content");
    }

    @Test
    void uploadsSelectedEmptyFile() throws Exception {
        verifyFileUpload(false, "");
    }

    private void verifyFileUpload(boolean mixed, String contents) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        List<ParameterDefinition> definitions = new ArrayList<>();
        definitions.add(new FileParameterDefinition("upload.txt", "Upload description"));
        if (mixed) {
            definitions.add(new StringParameterDefinition("message", "default"));
            definitions.add(new hudson.model.BooleanParameterDefinition("enabled", true, ""));
        }
        project.addProperty(new ParametersDefinitionProperty(definitions));
        java.nio.file.Path upload = java.nio.file.Files.createTempFile("parameter-upload-", ".txt");
        try (WebClient wc = j.createWebClient()) {
            java.nio.file.Files.writeString(upload, contents);
            HtmlPage page = wc.getPage(project, "parambuild?message=from-url");
            assertNull(project.getLastBuild());
            assertNull(j.jenkins.getQueue().getItem(project));
            assertEquals(1, page.getByXPath("//input[@type='file']").size());
            org.htmlunit.html.HtmlFileInput input =
                    (org.htmlunit.html.HtmlFileInput) page.getByXPath("//input[@type='file']").get(0);
            input.setValue(upload.toString());
            HtmlForm form = page.getFormByName("config");
            if (mixed) {
                form.getInputByName("message").setValue("edited");
                ((org.htmlunit.html.HtmlCheckBoxInput) form.getInputByName("enabled")).setChecked(false);
            }
            DomElement submit = page.createElement("button");
            submit.setAttribute("type", "submit");
            form.appendChild(submit);
            submit.click();
            for (int i = 0; project.getLastBuild() == null && i < 50; i++) {
                Thread.sleep(100);
            }
            j.waitUntilNoActivity();
            assertNotNull(project.getLastBuild());
            j.assertBuildStatusSuccess(project.getLastBuild());
            ParametersAction parameters = project.getLastBuild().getAction(ParametersAction.class);
            hudson.model.FileParameterValue value =
                    (hudson.model.FileParameterValue) parameters.getParameter("upload.txt");
            assertNotNull(value);
            assertEquals("Upload description", value.getDescription());
            assertEquals(upload.getFileName().toString(), value.getOriginalFileName());
            assertEquals(contents, project.getLastBuild().getWorkspace().child("upload.txt").readToString());
            if (mixed) {
                assertEquals("edited", ((StringParameterValue) parameters.getParameter("message")).value);
                assertFalse(((hudson.model.BooleanParameterValue) parameters.getParameter("enabled")).value);
            }
        } finally {
            java.nio.file.Files.deleteIfExists(upload);
        }
    }

    @Test
    void getAvailableParameters_passwordParam() throws IOException {
        ParameterDefinition pwParamDef = new PasswordParameterDefinition("n", BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER, "d");
        BuildWithParametersAction bwpa = testableProject(pwParamDef);

        BuildParameter pwParameter = (BuildParameter) bwpa.getAvailableParameters().get(0);
        assertSame(BuildParameterType.PASSWORD, pwParameter.getType());
    }

    private BuildWithParametersAction testableProject(
            ParameterDefinition pwParamDef) throws IOException {
        FreeStyleProject project = j.createFreeStyleProject();
        ParametersDefinitionProperty paramsDef = new ParametersDefinitionProperty(pwParamDef);
        project.addProperty(paramsDef);
        return new BuildWithParametersAction(project) {
            @Override
            ParameterValue getParameterDefinitionValue(
                    ParameterDefinition parameterDefinition) {
                return null;
            }
        };
    }

    @Test
    void applyDefaultPassword() throws IOException {
        String jobDefaultPassword = "defaultPassword";
        String passwordFromRequest = BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        String adjustedPassword = applyDefaultPasswordHelper(jobDefaultPassword, passwordFromRequest);

        assertEquals(jobDefaultPassword, adjustedPassword);
    }

    @Test
    void applyDefaultPassword_nonDefault() throws IOException {
        String jobDefaultPassword = "defaultPassword";
        String passwordFromRequest = "userSuppliedPassword";
        String adjustedPassword = applyDefaultPasswordHelper(jobDefaultPassword, passwordFromRequest);

        assertEquals(passwordFromRequest, adjustedPassword);
    }

    private String applyDefaultPasswordHelper(String jobDefaultPassword, String passwordFromRequest) throws IOException {
        PasswordParameterDefinition pwParamDef = new PasswordParameterDefinition("n", jobDefaultPassword, "d");
        BuildWithParametersAction bwpa = testableProject(pwParamDef);

        PasswordParameterValue parameterValue = new PasswordParameterValue("n", passwordFromRequest);

        ParameterValue adjustedParamValue = bwpa.applyDefaultPassword(pwParamDef, parameterValue);
        return BuildWithParametersAction.getPasswordValue((PasswordParameterValue)adjustedParamValue);
    }

    @Test
    void rendersFormattedParameterDescriptionLikeStandardBuildForm() throws Exception {
        String description = "<strong id=\"parameter-description\">Choose a release</strong>";
        String name = "<strong id=\"parameter-name\">release</strong>";
        // Allow only this fixed HTML fixture, without requiring an extra formatter plugin.
        j.jenkins.setMarkupFormatter(new MarkupFormatter() {
            @Override
            public void translate(String markup, Writer output) throws IOException {
                output.write(description.equals(markup) ? description : Util.escape(markup));
            }
        });
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition(name, "default", description)));

        try (WebClient wc = j.createWebClient()) {
            // Jenkins intentionally returns 405 for the standard build confirmation form.
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.getOptions().setPrintContentOnFailingStatusCode(false);
            HtmlPage standardForm = wc.getPage(project, "build?delay=0sec");
            assertEquals(405, standardForm.getWebResponse().getStatusCode());
            assertEquals(1, standardForm.getByXPath("//strong[@id='parameter-description']").size());

            wc.getOptions().setThrowExceptionOnFailingStatusCode(true);
            HtmlPage confirmationForm = wc.getPage(project, "parambuild");
            assertEquals(1, confirmationForm.getByXPath("//strong[@id='parameter-description']").size());
            assertFalse(confirmationForm.asNormalizedText().contains(description));
            assertEquals(0, confirmationForm.getByXPath("//strong[@id='parameter-name']").size());
            assertTrue(confirmationForm.asNormalizedText().contains(name));
            assertEquals("default", confirmationForm.getFormByName("config").getInputByName(name).getValue());
        }
        assertNull(project.getLastBuild());
        assertEquals(0, j.jenkins.getQueue().getItems().length);
    }

    @Test
    void escapesParameterDescriptionWithPlainTextFormatter() throws Exception {
        j.jenkins.setMarkupFormatter(new EscapedMarkupFormatter());
        String description = "<strong id=\"parameter-description\">Choose a release</strong> & confirm";
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("release", "default", description)));

        try (WebClient wc = j.createWebClient()) {
            HtmlPage page = wc.getPage(project, "parambuild");
            assertEquals(0, page.getByXPath("//strong[@id='parameter-description']").size());
            assertTrue(page.asNormalizedText().contains(description));
        }
        assertNull(project.getLastBuild());
        assertEquals(0, j.jenkins.getQueue().getItems().length);
    }

    @Test
    void rendersAndSubmitsNodeAndLabelParametersUsingTheirOwnControls() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        List<String> allowedNodes = new ArrayList<>();
        allowedNodes.add("built-in");
        project.addProperty(new ParametersDefinitionProperty(
                new NodeParameterDefinition("NODE", "Select a node", "built-in", allowedNodes, null),
                new LabelParameterDefinition("LABEL", "Select a label", "built-in")));

        try (WebClient wc = j.createWebClient()) {
            wc.getOptions().setJavaScriptEnabled(true);
            assertNotNull(j.jenkins.pluginManager.getPlugin("nodelabelparameter"));
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.getOptions().setPrintContentOnFailingStatusCode(false);
            HtmlPage standardForm = wc.getPage(project, "build?delay=0sec");
            assertEquals(405, standardForm.getWebResponse().getStatusCode());
            assertEquals(1, standardForm.getByXPath("//select[@name='value']").size());

            wc.getOptions().setThrowExceptionOnFailingStatusCode(true);
            HtmlPage page = wc.getPage(project, "parambuild");
            HtmlSelect nodeSelect = (HtmlSelect) page.getByXPath(
                    "//div[@name='parameter'][input[@name='name' and @value='NODE']]//select[@name='value']").get(0);
            HtmlInput labelInput = (HtmlInput) page.getByXPath(
                    "//div[@name='parameter'][input[@name='name' and @value='LABEL']]//input[@name='value']").get(0);
            nodeSelect.setSelectedAttribute("built-in", true);
            labelInput.setValue("built-in");

            HtmlForm form = page.getFormByName("config");
            DomElement submit = page.createElement("button");
            submit.setAttribute("type", "submit");
            form.appendChild(submit);
            submit.click();
        }

        Queue.Item item = j.jenkins.getQueue().getItem(project);
        for (int i = 0; item == null && project.getLastBuild() == null && i < 50; i++) {
            Thread.sleep(100);
            item = j.jenkins.getQueue().getItem(project);
        }
        ParametersAction parameters = item != null
                ? item.getAction(ParametersAction.class)
                : project.getLastBuild().getAction(ParametersAction.class);
        assertEquals("built-in", ((NodeParameterValue) parameters.getParameter("NODE")).getLabel());
        assertEquals("built-in", ((LabelParameterValue) parameters.getParameter("LABEL")).getLabel());
    }

    @Test
    void rendersAndSubmitsSingleValidatingStringParameter() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(validatingIpParameter()));

        try (WebClient wc = j.createWebClient()) {
            HtmlPage page = wc.getPage(project, "parambuild?ip=10.10.10.10");
            HtmlInput input = (HtmlInput) page.getByXPath(
                    "//div[@name='parameter'][input[@name='name' and @value='ip']]//input[@name='value']").get(0);
            assertTrue(input.getAttribute("checkUrl").contains("validate"));
            input.setValue("10.10.10.10");

            HtmlForm form = page.getFormByName("config");
            DomElement submit = page.createElement("button");
            submit.setAttribute("type", "submit");
            form.appendChild(submit);
            submit.click();
        }

        for (int i = 0; project.getLastBuild() == null && i < 50; i++) {
            Thread.sleep(100);
        }
        assertNotNull(project.getLastBuild());
        ParametersAction parameters = project.getLastBuild().getAction(ParametersAction.class);
        assertEquals("10.10.10.10", ((StringParameterValue) parameters.getParameter("ip")).value);
    }

    @Test
    void rejectsInvalidSingleValidatingStringParameter() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(validatingIpParameter()));

        try (WebClient wc = j.createWebClient()) {
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage page = wc.getPage(project, "parambuild");
            HtmlInput input = (HtmlInput) page.getByXPath(
                    "//div[@name='parameter'][input[@name='name' and @value='ip']]//input[@name='value']").get(0);
            input.setValue("not-an-ip");

            HtmlForm form = page.getFormByName("config");
            DomElement submit = page.createElement("button");
            submit.setAttribute("type", "submit");
            form.appendChild(submit);
            HtmlPage response = submit.click();
            assertTrue(response.getWebResponse().getStatusCode() >= 400);
        }

        assertNull(project.getLastBuild());
        assertNull(j.jenkins.getQueue().getItem(project));
    }

    private ValidatingStringParameterDefinition validatingIpParameter() {
        return new ValidatingStringParameterDefinition(
                "ip", "10.0.0.1", "\\d+\\.\\d+\\.\\d+\\.\\d+", "Invalid IP", "IPv4");
    }

    @Test
    void provideParametersViaUi() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        StringParameterDefinition params = new StringParameterDefinition("param", "default");
        project.addProperty(new ParametersDefinitionProperty(params));

        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(project, "parambuild");
        HtmlForm form = page.getFormByName("config");
        form.getInputByName("param").setValue("newValue");

        // This does not submit the form for some reason.
        HtmlFormUtil.getButtonByCaption(form, "Build").click();
        // Create fake submit instead
        DomElement fakeSubmit = page.createElement("button");
        fakeSubmit.setAttribute("type", "submit");
        form.appendChild(fakeSubmit);
        fakeSubmit.click();

        do {
            Thread.sleep(100);
        } while (project.getLastBuild() == null);

        final ParametersAction parameterAction = project.getLastBuild().getAction(ParametersAction.class);
        final String actualValue = ((StringParameterValue) parameterAction.getParameter("param")).value;
        assertEquals("newValue", actualValue);
    }

    @Test
    void skipsParameterWithoutSubmittedValue() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(new FileParameterDefinition("file")));

        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(project, "parambuild");
        HtmlForm form = page.getFormByName("config");
        DomElement submit = page.createElement("button");
        submit.setAttribute("type", "submit");
        form.appendChild(submit);
        submit.click();

        j.waitUntilNoActivity();
        ParametersAction parameters = project.getLastBuild().getAction(ParametersAction.class);
        assertEquals(0, parameters.getParameters().size());
    }
}
