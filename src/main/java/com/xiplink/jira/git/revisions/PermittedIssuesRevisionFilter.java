package com.xiplink.jira.git.revisions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.DocIdBitSet;

import java.io.IOException;
import java.util.BitSet;
import java.util.Set;

public class PermittedIssuesRevisionFilter extends AbstractRevisionFilter
{
    private final Set<String> permittedIssueKeys;

    public PermittedIssuesRevisionFilter(IssueManager issueManager, PermissionManager permissionManager, User user, Set<String> permittedIssueKeys)
    {
        super(issueManager, permissionManager, user);
        this.permittedIssueKeys = permittedIssueKeys;
    }

    @Override
    public DocIdSet getDocIdSet(IndexReader indexReader) throws IOException
    {
        BitSet bitSet = new BitSet(indexReader.maxDoc());

        for (String issueKey : permittedIssueKeys)
        {
            TermDocs termDocs = indexReader.termDocs(new Term(RevisionIndexer.FIELD_ISSUEKEY, issueKey));

            while (termDocs.next())
                bitSet.set(termDocs.doc(), true);
        }

        return new DocIdBitSet(bitSet);
    }
}
