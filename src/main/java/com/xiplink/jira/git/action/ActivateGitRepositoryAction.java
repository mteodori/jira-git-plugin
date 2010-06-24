package com.xiplink.jira.git.action;

import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;


public class ActivateGitRepositoryAction extends GitActionSupport {
	private long repoId;
	private GitManager gitManager;

	public ActivateGitRepositoryAction(MultipleGitRepositoryManager manager) {
		super(manager);
	}

	public String getRepoId() {
		return Long.toString(repoId);
	}

	public void setRepoId(String repoId) {
		this.repoId = Long.parseLong(repoId);
	}

	public String doExecute() {
		if (!hasPermissions()) {
			return PERMISSION_VIOLATION_RESULT;
		}

		gitManager = getMultipleRepoManager().getRepository(repoId);
		gitManager.activate();
		if (!gitManager.isActive()) {
			addErrorMessage(getText("git.repository.activation.failed",
					gitManager.getInactiveMessage()));
		}
		return SUCCESS;
	}

	public GitManager getGitManager() {
		return gitManager;
	}

}
