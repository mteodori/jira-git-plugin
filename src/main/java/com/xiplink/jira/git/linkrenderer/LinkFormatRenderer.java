package com.xiplink.jira.git.linkrenderer;

import com.atlassian.core.util.map.EasyMap;
import org.apache.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import com.atlassian.core.util.StringUtils;
import com.xiplink.jira.git.FileDiff;
import com.xiplink.jira.git.GitConstants;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.ViewLinkFormat;

import java.util.Map;

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
    public String getCopySrcLink(RevCommit revision, FileDiff path) {
        return formatLink(pathLinkFormat, path.path, EasyMap.build(
                "${rev}", revision.getId().name(),
                "${path}", path.path));
    }

    public String getRevisionLink(RevCommit revision) {
        // TODO
        return getRevisionLink(revision.getId().getName());
    }

    public String getChangePathLink(RevCommit revision, FileDiff path) {
        String changeType = path.change;

        Map<String, String> subst = EasyMap.build(
                "${rev}", revision.getId().name(),
                "${path}", path.path,
                "${parent}", revision.getParent(0).getId().name()
        );

        String format;
        if (GitConstants.MODIFICATION.equals(changeType)) {
            format = fileModifiedFormat;
        } else if (GitConstants.ADDED.equals(changeType)) {
            subst.put("${blob}", path.blobs[1].name());
            format = fileAddedFormat;
        } else if (GitConstants.REPLACED.equals(changeType)) {
            format = fileReplacedFormat;
        } else if (GitConstants.DELETED.equals(changeType)) {
            subst.put("${blob}", path.blobs[0].name());
            format = fileDeletedFormat;
        } else {
            format = fileModifiedFormat;
        }

        return formatLink(format, path.path, subst);
    }

    protected String getRevisionLink(String revisionNumber) {
        if (changesetFormat != null) {
            try {
                String href = StringUtils.replaceAll(changesetFormat, "${rev}", "" + revisionNumber);
                String shortRevNumber = revisionNumber.substring(0, 7);
                return "<a href=\"" + href + "\">" + shortRevNumber + "...</a>";
            } catch (Exception ex) {
                log.error("format error: " + ex.getMessage(), ex);
            }
        }
        return "" + revisionNumber;

    }

    private String formatLink(String format, String path, Map<String, String> substitutions) {

        if (format != null) {
            String href = format;

            for (Map.Entry<String, String> subst : substitutions.entrySet()) {
                href = StringUtils.replaceAll(href, subst.getKey(), subst.getValue());
            }

            return String.format("<a href=\"%s\">%s</a>", href, path);
        }

        return path;
    }
}
