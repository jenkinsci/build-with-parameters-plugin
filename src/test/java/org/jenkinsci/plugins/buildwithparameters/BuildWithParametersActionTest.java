package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import hudson.model.ParameterValue;
import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.FileParameterDefinition;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlPage;

public class BuildWithParametersActionTest {
    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void getAvailableParameters_passwordParam() throws IOException {
        ParameterDefinition pwParamDef = new PasswordParameterDefinition("n", BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER, "d");
        BuildWithParametersAction bwpa = testableProject(pwParamDef);

        BuildParameter pwParameter = (BuildParameter) bwpa.getAvailableParameters().get(0);
        assertSame(pwParameter.getType(), BuildParameterType.PASSWORD);
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
    public void applyDefaultPassword() throws IOException {
        String jobDefaultPassword = "defaultPassword";
        String passwordFromRequest = BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        String adjustedPassword = applyDefaultPasswordHelper(jobDefaultPassword, passwordFromRequest);

        assertEquals(jobDefaultPassword, adjustedPassword);
    }

    @Test
    public void applyDefaultPassword_nonDefault() throws IOException {
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
    public void provideParametersViaUi() throws Exception {
        StringParameterDefinition strParam = new StringParameterDefinition("str_param", "default", "desc");
        BooleanParameterDefinition boolParam = new BooleanParameterDefinition("bool_param", false, "desc");
        FileParameterDefinition fileParam = new FileParameterDefinition("file_param");
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(strParam, boolParam, fileParam));

        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(project, "parambuild?str_param=newValue&bool_param=true");
        HtmlForm form = page.getFormByName("config");

        form.getInputByName(strParam.getName()).setValue("evenNewerValue");
        // TODO: set the bool and file params? keep some, to ensure the query param comes through? handle other param types?

        // This does not submit the form for some reason.
        HtmlFormUtil.getButtonByCaption(form, "Build").click();
        // Create fake submit instead
        DomElement fakeSubmit = page.createElement("button");
        fakeSubmit.setAttribute("type", "submit");
        form.appendChild(fakeSubmit);
        fakeSubmit.click();

        FreeStyleBuild lastBuild = null;
        do {
            Thread.sleep(100);
            lastBuild = project.getLastBuild();
        } while (lastBuild == null);

        // ensure that it actually succeeded
        String buildStatusMessage = lastBuild.getBuildStatusSummary().message;
        assertEquals("stable", buildStatusMessage);

        // ensure that the correct parameters were built
        ParametersAction parameterAction = lastBuild.getAction(ParametersAction.class);
        String actualStrValue = ((StringParameterValue) parameterAction.getParameter("str_param")).value;
        assertEquals(actualStrValue, "evenNewerValue");
        boolean actualBoolValue = ((BooleanParameterValue) parameterAction.getParameter("bool_param")).value;
        assertEquals(actualBoolValue, true);
    }
}
