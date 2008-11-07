package com.xiplink.jira.git;

/**
 * User: detkin Date: 27/05/2005 Time: 13:11:18
 */
public class GitProperties implements GProperties {
	private String origin;
	private String root;
	private String displayName;
	private Boolean revisionIndexing;
	private Integer revisioningCacheSize;
	private String changeSetFormat;
	private String webLinkType;
	private String viewFormat;
	private String fileAddedFormat;
	private String fileDeletedFormat;
	private String fileModifiedFormat;
	private String fileReplacedFormat;

	public void fillPropertiesFromOther(GitProperties other) {
		if (this.getRevisionIndexing() == null) {
			this.revisionIndexing = other.getRevisionIndexing();
		}
		if (this.getRevisionCacheSize() == null) {
			this.revisioningCacheSize = other.getRevisionCacheSize();
		}

		if (origin == null) {
			setOrigin(other.getOrigin());
		}

		if (webLinkType == null)
			setWebLinkType(other.getWebLinkType());

		if (changeSetFormat == null)
			setChangeSetFormat(other.getChangesetFormat());

		if (viewFormat == null)
			setViewFormat(other.getViewFormat());

		if (fileAddedFormat == null)
			setFileAddedFormat(other.getFileAddedFormat());

		if (fileDeletedFormat == null)
			setFileDeletedFormat(other.getFileDeletedFormat());

		if (fileModifiedFormat == null)
			setFileModifiedFormat(other.getFileModifiedFormat());

		if (fileReplacedFormat == null)
			setFileReplacedFormat(other.getFileReplacedFormat());
	}

	public String getWebLinkType() {
		return webLinkType;
	}

	public void setWebLinkType(String webLinkType) {
		this.webLinkType = webLinkType;
	}

	public String toString() {
		return "origin: " + origin + "  revisioningIndex: " + getRevisionIndexing() + " revisioningCacheSize: "
				+ getRevisionCacheSize();
	}

	public String getRoot() {
		return root;
	}

	public String getOrigin() {
		return origin;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Boolean getRevisionIndexing() {
		return revisionIndexing;
	}

	public Integer getRevisionCacheSize() {
		return revisioningCacheSize;
	}

	public String getChangesetFormat() {
		return changeSetFormat;
	}

	public String getViewFormat() {
		return viewFormat;
	}

	public String getFileAddedFormat() {
		return fileAddedFormat;
	}

	public String getFileDeletedFormat() {
		return fileDeletedFormat;
	}

	public String getFileModifiedFormat() {
		return fileModifiedFormat;
	}

	public String getFileReplacedFormat() {
		return fileReplacedFormat;
	}

	public GitProperties setRoot(String root) {
		this.root = root;
		return this;
	}

	public GitProperties setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public GitProperties setRevisionIndexing(Boolean revisionIndexing) {
		this.revisionIndexing = revisionIndexing;
		return this;
	}

	public GitProperties setRevisioningCacheSize(Integer revisioningCacheSize) {
		this.revisioningCacheSize = revisioningCacheSize;
		return this;
	}

	public GitProperties setChangeSetFormat(String changeSetFormat) {
		this.changeSetFormat = changeSetFormat;
		return this;
	}

	public GitProperties setViewFormat(String viewFormat) {
		this.viewFormat = viewFormat;
		return this;
	}

	public GitProperties setFileAddedFormat(String fileAddedFormat) {
		this.fileAddedFormat = fileAddedFormat;
		return this;
	}

	public GitProperties setFileDeletedFormat(String fileDeletedFormat) {
		this.fileDeletedFormat = fileDeletedFormat;
		return this;
	}

	public GitProperties setFileModifiedFormat(String fileModifiedFormat) {
		this.fileModifiedFormat = fileModifiedFormat;
		return this;
	}

	public GitProperties setFileReplacedFormat(String fileReplacedFormat) {
		this.fileReplacedFormat = fileReplacedFormat;
		return this;
	}

	public GitProperties setOrigin(String gitOriginStr) {
		this.origin = gitOriginStr;
		return this;
	}

}
