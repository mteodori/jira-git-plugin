package com.xiplink.jira.git.revisions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.search.Filter;

public abstract class AbstractRevisionFilter extends Filter
{
    final IssueManager issueManager;

    final PermissionManager permissionManager;

    final User user;

    final FieldSelector issueKeysFieldSelector;

    public AbstractRevisionFilter(IssueManager issueManager, PermissionManager permissionManager, User user)
    {
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.user = user;
        issueKeysFieldSelector = new FieldSelector()
        {
            public FieldSelectorResult accept(String s)
            {
                return StringUtils.equals(s, RevisionIndexer.FIELD_ISSUEKEY)
                        ? FieldSelectorResult.LOAD
                        : FieldSelectorResult.NO_LOAD;
            }
        };
    }
}
