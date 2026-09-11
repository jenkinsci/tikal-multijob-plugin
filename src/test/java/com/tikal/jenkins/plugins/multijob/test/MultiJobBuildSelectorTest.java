package com.tikal.jenkins.plugins.multijob.test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuildSelector;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.copyartifact.BuildFilter;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MultiJobBuildSelectorTest {

    @Test
    void selectsRecordedBuildWhenSourceJobHasCustomDisplayName(JenkinsRule j) throws Exception {
        FreeStyleProject source = j.createFreeStyleProject("source-job");

        // Custom display name must not affect the identity of the Jenkins job.
        source.setDisplayName("Readable Source Job");

        FreeStyleBuild expectedBuild = j.buildAndAssertSuccess(source);

        // Create another newer build to ensure that the selector resolves
        // the exact build recorded by the MultiJob rather than the latest build.
        j.buildAndAssertSuccess(source);

        MultiJobProject multiJob = j.createProject(MultiJobProject.class, "multi-job");

        MultiJobBuild multiJobBuild =
                j.assertBuildStatus(Result.SUCCESS, multiJob.scheduleBuild2(0).get());

        /*
         * MultiJobBuilder records the source job using Job#getName().
         * Simulate the SubBuild information recorded by a real MultiJob phase.
         */
        multiJobBuild.addSubBuild(new MultiJobBuild.SubBuild(
                multiJob.getName(),
                multiJobBuild.getNumber(),
                source.getName(),
                "",
                expectedBuild.getNumber(),
                "compile",
                Result.SUCCESS,
                "",
                "",
                expectedBuild.getUrl(),
                expectedBuild));

        MultiJobBuildSelector selector = new MultiJobBuildSelector();

        Run<?, ?> selectedBuild = selector.getBuild(source, new EnvVars(), new BuildFilter(), multiJobBuild);

        // Verify the test actually exercises the custom-display-name case.
        assertNotEquals(source.getName(), source.getDisplayName());

        // The selector must resolve the exact build recorded by the MultiJob.
        assertSame(expectedBuild, selectedBuild);
    }
}
