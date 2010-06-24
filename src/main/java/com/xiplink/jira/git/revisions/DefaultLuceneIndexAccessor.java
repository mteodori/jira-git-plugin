package com.xiplink.jira.git.revisions;

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
class DefaultLuceneIndexAccessor implements LuceneIndexAccessor
{

    public IndexReader getIndexReader(String path) throws IndexException
    {
        return LuceneUtils.getIndexReader(path);
    }

    public IndexWriter getIndexWriter(String path, boolean create, Analyzer analyzer) throws IndexException
    {
        return LuceneUtils.getIndexWriter(path, create, analyzer);
    }
}
