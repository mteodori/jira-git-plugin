package com.xiplink.jira.git.linkrenderer;

import com.atlassian.core.util.map.EasyMap;
import org.apache.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import com.xiplink.jira.git.FileDiff;
import com.xiplink.jira.git.GitConstants;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.ViewLinkFormat;

import java.util.Map;
import org.apache.commons.lang.StringUtils;

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
    private String fileAddedFormat;
    private String fileModifiedFormat;
    private String fileDeletedFormat;
    private String changesetFormat;

    public LinkFormatRenderer(GitManager gitManager) {

        ViewLinkFormat linkFormat = gitManager.getViewLinkFormat();

        if (linkFormat != null) {
            if (StringUtils.isNotBlank(linkFormat.getChangesetFormat())) {
                changesetFormat = linkFormat.getChangesetFormat();
            }

            if (StringUtils.isNotBlank(linkFormat.getFileAddedFormat())) {
                fileAddedFormat = linkFormat.getFileAddedFormat();
            }

            if (StringUtils.isNotBlank(linkFormat.getFileModifiedFormat())) {
                fileModifiedFormat = linkFormat.getFileModifiedFormat();
            }

            if (StringUtils.isNotBlank(linkFormat.getFileDeletedFormat())) {
                fileDeletedFormat = linkFormat.getFileDeletedFormat();
            }

            if (StringUtils.isNotBlank(linkFormat.getViewFormat())) {
                pathLinkFormat = linkFormat.getViewFormat();
            }
        }
    }

    // TODO
    public String getCopySrcLink(RevCommit revision, FileDiff path) {
        return formatLink(pathLinkFormat, path.path, EasyMap.build(
                "${rev}", revision.getId().name(),
                "${path}", path.path));
    }

    public String getRevisionLink(RevCommit revision) {
        return getRevisionLink(revision.getId().getName());
    }

    public String getChangePathLink(RevCommit revision, FileDiff path) {
        String changeType = path.change;

        Map<String, String> subst = EasyMap.build(
                "${rev}", revision.getId().name(),
                "${path}", path.path,
                "${parent}", revision.getParent(0).getId().name()
        );

        if (path.blobs.length == 1) {
            subst.put("${blob}", path.blobs[0].name());
        } else if (path.blobs.length != 0) {
            subst.put("${blob}", path.blobs[1].name());
            subst.put("${parent_blob}", path.blobs[0].name());
        }

        String format;
        if (GitConstants.MODIFICATION.equals(changeType)) {
            format = fileModifiedFormat;
        } else if (GitConstants.ADDED.equals(changeType)) {
            format = fileAddedFormat;
        } else if (GitConstants.DELETED.equals(changeType)) {
            format = fileDeletedFormat;
        } else {
            format = fileModifiedFormat;
        }

        return formatLink(format, path.path, subst);
    }

    protected String getRevisionLink(String revisionNumber) {
        if (changesetFormat == null) {
            return revisionNumber;
        }

        String href = StringUtils.replace(changesetFormat, "${rev}", revisionNumber);
        String shortRevNumber = revisionNumber.substring(0, 7);
        return "<a href=\"" + href + "\">" + shortRevNumber + "...</a>";
    }

    private String formatLink(String format, String path, Map<String, String> substitutions) {
        if (format == null) {
            return path;
        }

        String href = format;

        for (Map.Entry<String, String> subst : substitutions.entrySet()) {
            href = StringUtils.replace(href, subst.getKey(), subst.getValue());
        }

        return String.format("<a href=\"%s\">%s</a>", href, path);
    }
}
