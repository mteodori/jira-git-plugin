package com.xiplink.jira.git.revisions;

import com.atlassian.jira.InfrastructureException;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.util.LuceneUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 * An implementation that uses JIRA's LuceneUtils for its guts.
 *
 * @since 0.9.12
 */
class DefaultLuceneIndexAccessor implements LuceneIndexAccessor {

    public IndexReader getIndexReader(String path) {
        try {
            return LuceneUtils.getIndexReader(path);
        } catch (IndexException e) {
            throw new InfrastructureException(e);
        }
    }

    public IndexWriter getIndexWriter(String path, boolean create, Analyzer analyzer) {
        try {
            return LuceneUtils.getIndexWriter(path, create, analyzer);
        } catch (IndexException e) {
            throw new InfrastructureException(e);
        }
    }
}
