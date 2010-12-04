package com.xiplink.jira.git.linkrenderer;

import com.atlassian.core.util.map.EasyMap;
import org.apache.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import com.xiplink.jira.git.FileDiff;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.ViewLinkFormat;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;

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
        return formatLink(pathLinkFormat, path.getPath(), EasyMap.build(
                "${rev}", revision.getId().name(),
                "${path}", path.getPath()));
    }

    public String getRevisionLink(RevCommit revision) {
        return getRevisionLink(revision.getId().getName());
    }

    public String getChangePathLink(RevCommit revision, FileDiff path) {
        Map<String, String> subst = EasyMap.build(
                "${num}", Integer.toString(path.getNumber()),
                "${rev}", revision.getId().name(),
                "${path}", path.getPath(),
                "${parent}", revision.getParent(0).getId().name()
        );

        ObjectId[] blobs = path.getBlobs();
        if (blobs.length == 1) {
            subst.put("${blob}", blobs[0].name());
        } else if (blobs.length != 0) {
            subst.put("${blob}", blobs[1].name());
            subst.put("${parent_blob}", blobs[0].name());
        }

        String format;
        switch (path.getChange()) {
            case MODIFY:
                format = fileModifiedFormat;
                break;
            case ADD:
                format = fileAddedFormat;
                break;
            case DELETE:
                format = fileDeletedFormat;
                break;
            default:
                format = fileModifiedFormat;
        }

        return formatLink(format, path.getPath(), subst);
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
