package com.xiplink.jira.git.linkrenderer;

import org.apache.log4j.Logger;
import org.spearce.jgit.revwalk.RevCommit;

import com.atlassian.core.util.StringUtils;
import com.xiplink.jira.git.FileDiff;
import com.xiplink.jira.git.GitConstants;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.ViewLinkFormat;

/**
 * A link renderer implementation which lets the user specify the format in the properties file, to accommodate various
 * formats (ViewCVS, Fisheye, etc) out there.
 * 
 * @author Chenggong Lu
 * @author Jeff Turner
 */
public class LinkFormatRenderer implements GitLinkRenderer {
	private static Logger log = Logger.getLogger(LinkFormatRenderer.class);
	private String pathLinkFormat;
	private String fileReplacedFormat;
	private String fileAddedFormat;
	private String fileModifiedFormat;
	private String fileDeletedFormat;
	private String changesetFormat;

	public LinkFormatRenderer(GitManager gitManager) {

		ViewLinkFormat linkFormat = gitManager.getViewLinkFormat();
		if (linkFormat != null) {
			if (linkFormat.getChangesetFormat() != null && linkFormat.getChangesetFormat().trim().length() != 0) {
				changesetFormat = linkFormat.getChangesetFormat();
			}

			if (linkFormat.getFileAddedFormat() != null && linkFormat.getFileAddedFormat().trim().length() != 0) {
				fileAddedFormat = linkFormat.getFileAddedFormat();
			}

			if (linkFormat.getFileModifiedFormat() != null && linkFormat.getFileModifiedFormat().trim().length() != 0) {
				fileModifiedFormat = linkFormat.getFileModifiedFormat();
			}

			if (linkFormat.getFileReplacedFormat() != null && linkFormat.getFileReplacedFormat().trim().length() != 0) {
				fileReplacedFormat = linkFormat.getFileReplacedFormat();
			}

			if (linkFormat.getFileDeletedFormat() != null && linkFormat.getFileDeletedFormat().trim().length() != 0) {
				fileDeletedFormat = linkFormat.getFileDeletedFormat();
			}

			if (linkFormat.getViewFormat() != null && linkFormat.getViewFormat().trim().length() != 0) {
				pathLinkFormat = linkFormat.getViewFormat();
			}

		} else {
			log.warn("viewLinkFormat is null");
		}
	}

	// TODO
	public String getCopySrcLink(RevCommit revision, FileDiff logEntryPath) {
		String revisionNumber = revision.getId().name();
		return linkPath(pathLinkFormat, logEntryPath.path
		// .getCopyPath()
				, revisionNumber
				// getPathLink(logEntryPath.getCopyPath()
				, "");
	}

	public String getRevisionLink(RevCommit revision) {
		// TODO
		return getRevisionLink(revision.getId().name());
	}

	public String getChangePathLink(RevCommit revision, FileDiff path) {
		String changeType = path.change;

		String revisionNumber = revision.getId().name();

		if (GitConstants.MODIFICATION.equals(changeType)) {
			return linkPath(fileModifiedFormat, path.path, revisionNumber, "");
		} else if (GitConstants.ADDED.equals(changeType)) {
			// TODO validate blob
			String baseId = "";
			if (path.blobs.length > 1) {
				baseId = path.blobs[1].name();
			}
			return linkPath(fileAddedFormat, path.path, baseId, revisionNumber);
		} else if (GitConstants.REPLACED.equals(changeType)) {
			return linkPath(fileReplacedFormat, path.path, revisionNumber, "");
		} else if (GitConstants.DELETED.equals(changeType)) {
			return linkPath(fileDeletedFormat, path.path, revisionNumber, "");
		} else {
			return linkPath(fileReplacedFormat, path.path, revisionNumber, "");
		}
	}

	protected String getRevisionLink(String revisionNumber) {
		if (changesetFormat != null) {
			try {
				String href = StringUtils.replaceAll(changesetFormat, "${rev}", "" + revisionNumber);
				return "<a href=\"" + href + "\">" + revisionNumber + "</a>";
			} catch (Exception ex) {
				log.error("format error: " + ex.getMessage(), ex);
			}
		}
		return "" + revisionNumber;

	}

	private String linkPath(final String format, String path, String revisionNumber, String base) {
		if (format != null) {

			try {
				String href = format;
				if (path != null) {
					href = StringUtils.replaceAll(href, "${path}", path);
				}
				href = StringUtils.replaceAll(href, "${rev}", revisionNumber);
				href = StringUtils.replaceAll(href, "${base}", base);

				return "<a href=\"" + href + "\">" + path + "</a>";
			} catch (Exception ex) {
				log.error("format error: " + ex.getMessage(), ex);
			}
		}
		return path;
	}
}
