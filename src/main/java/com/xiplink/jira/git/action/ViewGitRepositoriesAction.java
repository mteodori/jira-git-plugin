package com.xiplink.jira.git.action;

import java.util.Collection;

import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;

/**
 * Manage 1 or more repositories
 */
public class ViewGitRepositoriesAction extends GitActionSupport
{

    public ViewGitRepositoriesAction(MultipleGitRepositoryManager manager)
    {
        super (manager);
    }

    public Collection<GitManager> getRepositories()
    {
        return getMultipleRepoManager().getRepositoryList();
    }
}
