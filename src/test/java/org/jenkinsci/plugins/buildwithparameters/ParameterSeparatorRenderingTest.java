package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Util;
import hudson.markup.MarkupFormatter;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import java.io.IOException;
import java.io.Writer;
import jenkins.plugins.parameter_separator.ParameterSeparatorDefinition;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ParameterSeparatorRenderingTest {

    private static final String DESCRIPTION = "<strong id=\"parameter-description\">Choose a release</strong>";
    private static final String SECTION_HEADER = "<strong id=\"separator-header\">BASIC SETTINGS</strong>";

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void rendersSeparatorAndFormattedDescriptionsLikeStandardBuildForm() throws Exception {
        j.jenkins.setMarkupFormatter(new MarkupFormatter() {
            @Override
            public void translate(String markup, Writer output) throws IOException {
                if (DESCRIPTION.equals(markup) || SECTION_HEADER.equals(markup)) {
                    output.write(markup);
                } else {
                    output.write(Util.escape(markup));
                }
            }
        });

        FreeStyleProject project = j.createFreeStyleProject();
        ParameterSeparatorDefinition separator = new ParameterSeparatorDefinition(
                "separator-section", "border-width: 0", SECTION_HEADER, "background-color: #7ea6d3");
        project.addProperty(new ParametersDefinitionProperty(
                separator, new StringParameterDefinition("branch", "main", DESCRIPTION)));

        try (WebClient wc = j.createWebClient()) {
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.getOptions().setPrintContentOnFailingStatusCode(false);
            HtmlPage standardForm = wc.getPage(project, "build?delay=0sec");
            assertEquals(405, standardForm.getWebResponse().getStatusCode());
            assertRendersStyledSeparatorAndDescription(standardForm);

            wc.getOptions().setThrowExceptionOnFailingStatusCode(true);
            HtmlPage confirmationForm = wc.getPage(project, "parambuild");
            assertRendersStyledSeparatorAndDescription(confirmationForm);
            assertEquals(0, confirmationForm.getByXPath("//input[@name='separator-section']").size());
            assertFalse(confirmationForm.asNormalizedText().contains(DESCRIPTION));
        }

        assertNull(project.getLastBuild());
        assertEquals(0, j.jenkins.getQueue().getItems().length);
    }

    private void assertRendersStyledSeparatorAndDescription(HtmlPage page) {
        assertEquals(1, page.getByXPath("//hr[@style='border-width: 0']").size());
        assertEquals(1, page.getByXPath("//strong[@id='separator-header' and text()='BASIC SETTINGS']").size());
        assertEquals(1, page.getByXPath("//strong[@id='parameter-description' and text()='Choose a release']").size());
        assertTrue(page.asNormalizedText().contains("BASIC SETTINGS"));
    }
}
