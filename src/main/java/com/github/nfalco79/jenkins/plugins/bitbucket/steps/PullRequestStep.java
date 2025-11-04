/*
 * Copyright 2023 Nikolas Falco
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.jenkins.plugins.bitbucket.steps;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.github.nfalco79.bitbucket.client.BitbucketCloudClient;
import com.github.nfalco79.bitbucket.client.Credentials;
import com.github.nfalco79.bitbucket.client.Credentials.CredentialsBuilder;
import com.github.nfalco79.bitbucket.client.model.PullRequest;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Failure;
import hudson.model.Run;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class PullRequestStep extends Step {

    private String credentialsId;
    private final String workspace;
    private final String repository;
    private final Integer prId;

    @DataBoundConstructor
    public PullRequestStep(@CheckForNull String workspace, @CheckForNull String repository, @CheckForNull Integer prId) {
        this.workspace = Util.fixEmptyAndTrim(workspace);
        if (workspace == null) {
            throw new Failure(Messages.Steps_requiredParameter("Workspace/Owner"));
        }
        this.repository = Util.fixEmptyAndTrim(repository);
        if (repository == null) {
            throw new Failure(Messages.Steps_requiredParameter("Repository"));
        }
        this.prId = prId;
        if (prId == null) {
            throw new Failure(Messages.Steps_requiredParameter("Pull request identifier"));
        }
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getRepository() {
        return repository;
    }

    public Integer getPrId() {
        return prId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new PullRequestStepExecution(this, context);
    }

    private static class PullRequestStepExecution extends SynchronousStepExecution<PullRequest> {
        private static final long serialVersionUID = 1L;
        private final transient PullRequestStep step;

        private PullRequestStepExecution(PullRequestStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected PullRequest run() throws Exception {
            Credentials credentials = null;
            if (step.credentialsId != null) {
                String credentialsId = step.credentialsId;
                Run<?, ?> run = getContext().get(Run.class);
                StandardUsernamePasswordCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernamePasswordCredentials.class, run);
                if (c != null) {
                    credentials = CredentialsBuilder.build(c.getUsername(), c.getPassword().getPlainText());
                }
            }
            try (BitbucketCloudClient client = new BitbucketCloudClient(credentials)) {
                return client.getPullRequest(step.workspace, step.repository, step.prId);
            }
        }

    }

    @Extension
    public static class DescriptorImpl extends AbstractBitbucketStepDescriptor {
        @Override
        public String getFunctionName() {
            return "bitbucketPullRequest";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.PullRequestStep_displayName();
        }

        public FormValidation doCheckWorkspace(@QueryParameter String workspace) {
            return FormValidationBuilder.checkWorkspace(workspace);
        }

        public FormValidation doCheckRepository(@QueryParameter String repository) {
            return FormValidationBuilder.checkRepository(repository);
        }

        public FormValidation doCheckPrId(@QueryParameter Integer prId) {
            return FormValidationBuilder.checkPrId(true, prId);
        }

    }
}
