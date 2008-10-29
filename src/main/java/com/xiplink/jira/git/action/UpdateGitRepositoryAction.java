package com.xiplink.jira.git.action;

import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;


public class UpdateGitRepositoryAction extends AddGitRepositoryAction {
	private long repoId = -1;

	public UpdateGitRepositoryAction(MultipleGitRepositoryManager multipleRepoManager) {
		super(multipleRepoManager);
	}

	public String doDefault() {
		if (ERROR.equals(super.doDefault()))
			return ERROR;

        if (!hasPermissions())
        {
        	addErrorMessage(getText("git.admin.privilege.required"));
            return PERMISSION_VIOLATION_RESULT;
        }


        if (repoId == -1) {
			addErrorMessage(getText("git.repository.id.missing"));
			return ERROR;
		}

		// Retrieve the repository
		final GitManager repository = getMultipleRepoManager().getRepository(repoId);
		if (repository == null) {
			addErrorMessage(getText("git.repository.does.not.exist", Long.toString(repoId)));
			return ERROR;
		}

		this.setDisplayName(repository.getDisplayName());
		this.setRoot(repository.getRoot());
		if (repository.getViewLinkFormat() != null) {
            this.setWebLinkType(repository.getViewLinkFormat().getType());
            this.setChangesetFormat(repository.getViewLinkFormat().getChangesetFormat());
			this.setViewFormat(repository.getViewLinkFormat().getViewFormat());
			this.setFileAddedFormat(repository.getViewLinkFormat().getFileAddedFormat());
			this.setFileDeletedFormat(repository.getViewLinkFormat().getFileDeletedFormat());
			this.setFileModifiedFormat(repository.getViewLinkFormat().getFileModifiedFormat());
			this.setFileReplacedFormat(repository.getViewLinkFormat().getFileReplacedFormat());
		}
		this.setUsername(repository.getUsername());
		this.setPassword(repository.getPassword());
		this.setPrivateKeyFile(repository.getPrivateKeyFile());
		this.setRevisionCacheSize(new Integer(repository.getRevisioningCacheSize()));
		this.setRevisionIndexing(new Boolean(repository.isRevisionIndexing()));

		return INPUT;
	}

	public String doExecute() {
		if (!hasPermissions()) {
			addErrorMessage(getText("git.admin.privilege.required"));
			return ERROR;
		}

		if (repoId == -1) {
			return getRedirect("ViewGitRepositories.jspa");
		}

		GitManager gitManager = getMultipleRepoManager().updateRepository(repoId, this);
		if (!gitManager.isActive()) {
			repoId = gitManager.getId();
			addErrorMessage(gitManager.getInactiveMessage());
			addErrorMessage(getText("admin.errors.occured.when.updating"));
			return ERROR;
		}
		return getRedirect("ViewGitRepositories.jspa");
	}

	public long getRepoId() {
		return repoId;
	}

	public void setRepoId(long repoId) {
		this.repoId = repoId;
	}

}
