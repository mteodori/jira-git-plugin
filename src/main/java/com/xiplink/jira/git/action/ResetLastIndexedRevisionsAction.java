package com.xiplink.jira.git.action;

import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;


public class ResetLastIndexedRevisionsAction extends GitActionSupport {
	private long repoId;
	private GitManager gitManager;

	public ResetLastIndexedRevisionsAction(MultipleGitRepositoryManager manager) {
		super(manager);
	}

    public String getRepoId() {
		return Long.toString(repoId);
	}

	public void setRepoId(String repoId) {
		this.repoId = Long.parseLong(repoId);
	}

	public String doDefault() {
        if (!hasPermissions())
        {
            return PERMISSION_VIOLATION_RESULT;
        }

		gitManager = getMultipleRepoManager().getRepository(repoId);
		return INPUT;
	}

	public String doExecute() {
        if (!hasPermissions())
        {
            return PERMISSION_VIOLATION_RESULT;
        }

		getMultipleRepoManager().clearLastIndexedRevisions(repoId);
		return getRedirect("ViewGitRepositories.jspa");
	}

	public GitManager getGitManager() {
		return gitManager;
	}
}