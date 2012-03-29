package com.xiplink.jira.git.revisions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.DocIdBitSet;

import java.io.IOException;
import java.util.BitSet;

public class ProjectRevisionFilter extends AbstractRevisionFilter
{
    private final String projectKey;

    public ProjectRevisionFilter(IssueManager issueManager, PermissionManager permissionManager, User user, String projectKey)
    {
        super(issueManager, permissionManager, user);
        this.projectKey = projectKey;
    }

    @Override
    public DocIdSet getDocIdSet(IndexReader indexReader) throws IOException
    {
        BitSet bitSet = new BitSet(indexReader.maxDoc());

        TermDocs termDocs = indexReader.termDocs(new Term(RevisionIndexer.FIELD_PROJECTKEY, projectKey));
        while (termDocs.next())
        {
            int docId = termDocs.doc();
            Document theDoc = indexReader.document(docId, issueKeysFieldSelector);

            boolean allow = false;
            String[] issueKeys = theDoc.getValues(RevisionIndexer.FIELD_ISSUEKEY);

            if (null != issueKeys)
                for (String issueKey : issueKeys)
                {
                    Issue anIssue = issueManager.getIssueObject(StringUtils.upperCase(issueKey));
                    if (null != anIssue && permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, anIssue, user))
                    {
                        allow = true;
                        break;
                    }
                }

            bitSet.set(docId, allow);
        }

        return new DocIdBitSet(bitSet);
    }
}
