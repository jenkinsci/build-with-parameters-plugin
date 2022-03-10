package org.jenkinsci.plugins.buildwithparameters;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import jenkins.model.TransientActionFactory;

import java.util.Collection;
import java.util.Collections;

@Extension
public class ParamBuilderActionFactory extends TransientActionFactory<Job> {

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends Action> createFor(@NonNull Job target) {
        if (target instanceof ParameterizedJob) {
            return Collections.singletonList(new BuildWithParametersAction(target));
        } else {
            return Collections.emptyList();
        }
    }

}
