package com.xiplink.jira.git.action;
import com.opensymphony.util.TextUtils;
import com.xiplink.jira.git.GProperties;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;

public class AddGitRepositoryAction extends GitActionSupport implements GProperties {
	private String root;
	private String displayName;
	private String username;
	private String password;
	private String privateKeyFile;
	private Boolean revisionIndexing = Boolean.TRUE;
	private Integer revisionCacheSize = new Integer(10000);
    private String webLinkType;
    private String viewFormat;
	private String changesetFormat;
	private String fileAddedFormat;
	private String fileModifiedFormat;
	private String fileReplacedFormat;
	private String fileDeletedFormat;

	public AddGitRepositoryAction(MultipleGitRepositoryManager manager) {
		super(manager);
	}

	public void doValidation() {
		if (!TextUtils.stringSet(getDisplayName())) {
			addError("displayName", getText("git.errors.you.must.specify.a.name.for.the.repository"));
		}

		validateRepositoryParameters();
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root != null ? root.trim() : root;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		if (TextUtils.stringSet(username)) {
			this.username = username;
		} else {
			this.username = null;
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (TextUtils.stringSet(password)) {
			this.password = password;
		} else {
			this.password = null;
		}
	}

	public Boolean getRevisionIndexing() {
		return revisionIndexing;
	}

	public void setRevisionIndexing(Boolean revisionIndexing) {
		this.revisionIndexing = revisionIndexing;
	}

	public Integer getRevisionCacheSize() {
		return revisionCacheSize;
	}

	public void setRevisionCacheSize(Integer revisionCacheSize) {
		this.revisionCacheSize = revisionCacheSize;
	}

	public String getPrivateKeyFile() {
		return privateKeyFile;
	}

	public void setPrivateKeyFile(String privateKeyFile) {
		if (TextUtils.stringSet(privateKeyFile)) {
			this.privateKeyFile = privateKeyFile;
		} else {
			this.privateKeyFile = null;
		}
	}

    public String getWebLinkType() {
        return webLinkType;
    }

    public void setWebLinkType(String webLinkType) {
        this.webLinkType = webLinkType;
    }

    public String getChangesetFormat() {
		return changesetFormat;
	}

	public void setChangesetFormat(String changesetFormat) {
		if (TextUtils.stringSet(changesetFormat)) {
			this.changesetFormat = changesetFormat;
		} else {
			this.changesetFormat = null;
		}
	}

	public String getFileAddedFormat() {
		return fileAddedFormat;
	}

	public void setFileAddedFormat(String fileAddedFormat) {
		if (TextUtils.stringSet(fileAddedFormat)) {
			this.fileAddedFormat = fileAddedFormat;
		} else {
			this.fileAddedFormat = null;
		}
	}

	public String getViewFormat() {
		return viewFormat;
	}

	public void setViewFormat(String viewFormat) {
		if (TextUtils.stringSet(viewFormat)) {
			this.viewFormat = viewFormat;
		} else {
			this.viewFormat = null;
		}
	}

	public String getFileModifiedFormat() {
		return fileModifiedFormat;
	}

	public void setFileModifiedFormat(String fileModifiedFormat) {
		if (TextUtils.stringSet(fileModifiedFormat)) {
			this.fileModifiedFormat = fileModifiedFormat;
		} else {
			this.fileModifiedFormat = null;
		}
	}

	public String getFileReplacedFormat() {
		return fileReplacedFormat;
	}

	public void setFileReplacedFormat(String fileReplacedFormat) {
		if (TextUtils.stringSet(fileReplacedFormat)) {
			this.fileReplacedFormat = fileReplacedFormat;
		} else {
			this.fileReplacedFormat = null;
		}
	}

	public String getFileDeletedFormat() {
		return fileDeletedFormat;
	}

	public void setFileDeletedFormat(String fileDeletedFormat) {
		if (TextUtils.stringSet(fileDeletedFormat)) {
			this.fileDeletedFormat = fileDeletedFormat;
		} else {
			this.fileDeletedFormat = null;
		}
	}

	public String doExecute() throws Exception {
        if (!hasPermissions())
        {
            return PERMISSION_VIOLATION_RESULT;
        }

		GitManager GitManager = getMultipleRepoManager().createRepository(this);
		if (!GitManager.isActive()) {
			addErrorMessage(GitManager.getInactiveMessage());
			addErrorMessage(getText("admin.errors.occured.when.creating"));
			getMultipleRepoManager().removeRepository(GitManager.getId());
			return ERROR;
		}

		return getRedirect("ViewGitRepositories.jspa");
	}

	// This is public for testing purposes
	public void validateRepositoryParameters() {
		if (!TextUtils.stringSet(getDisplayName()))
			addError("displayName", getText("git.errors.you.must.specify.a.name.for.the.repository"));
		if (!TextUtils.stringSet(getRoot()))
			addError("root", getText("admin.errors.you.must.specify.the.root.of.the.repository"));
	}

}
