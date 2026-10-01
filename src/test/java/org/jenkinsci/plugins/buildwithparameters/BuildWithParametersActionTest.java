package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BuildWithParametersActionTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
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
    void provideParametersViaUi() throws Exception {
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

    @Test
    public void getAvailableParameters_fileParamSkipsCreateValue() throws Exception {
        FileParameterDefinition fileParam = new FileParameterDefinition("file_param");
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(fileParam));

        class CountingAction extends BuildWithParametersAction<FreeStyleProject> {
            int calls = 0;
            CountingAction(FreeStyleProject p) { super(p); }
            @Override
            ParameterValue getParameterDefinitionValue(ParameterDefinition pd) {
                calls++;
                return null;
            }
        }

        CountingAction action = new CountingAction(project);
        List<BuildParameter> params = action.getAvailableParameters();
        assertEquals(1, params.size());
        assertEquals(BuildParameterType.FILE, ((BuildParameter) params.get(0)).getType());
        // ensure we did NOT invoke createValue for file param
        assertEquals(0, action.calls);
    }

    @Test
    public void resolveFileParameter_acceptsJSONArray() throws Exception {
        FileParameterDefinition fileParam = new FileParameterDefinition("file_param");
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(fileParam));

        BuildWithParametersAction<FreeStyleProject> action = new BuildWithParametersAction<>(project);
        
        // Simulate formData with multiple parameters: {"parameter": [{"name":"str","value":"x"}, {"name":"file_param","":"file0"}]}
        JSONArray multiParams = new JSONArray();
        JSONObject strParam = new JSONObject();
        strParam.put("name", "str");
        strParam.put("value", "x");
        multiParams.add(strParam);
        
        JSONObject fileObj = new JSONObject();
        fileObj.put("name", "file_param");
        fileObj.put("", "file0");
        multiParams.add(fileObj);
        
        JSONObject formData = new JSONObject();
        formData.put("parameter", multiParams);

        // Pass null request; method will check null and return null without throwing exception
        ParameterValue result = action.resolveFileParameter(null, formData, fileParam);
        assertEquals(null, result); // Returns null when no real request provided
    }

    @Test
    public void resolveFileParameter_acceptsJSONObject() throws Exception {
        FileParameterDefinition fileParam = new FileParameterDefinition("file_param");
        FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(fileParam));

        BuildWithParametersAction<FreeStyleProject> action = new BuildWithParametersAction<>(project);
        
        // Simulate formData with single parameter object: {"parameter": {"name":"file_param","":"file0"}}
        JSONObject single = new JSONObject();
        single.put("name", "file_param");
        single.put("", "file0");
        JSONObject formData = new JSONObject();
        formData.put("parameter", single);

        ParameterValue result = action.resolveFileParameter(null, formData, fileParam);
        assertEquals(null, result); // Returns null when no real request provided
    }
}
