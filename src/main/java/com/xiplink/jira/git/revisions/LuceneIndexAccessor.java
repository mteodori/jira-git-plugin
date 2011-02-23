package com.xiplink.jira.git.revisions;

import com.atlassian.jira.issue.index.IndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 * Small abstraction for Lucene index reader and writer acquisition. Helps contain the dependencies of
 * RevisionIndexer on the internals of lucene and specifically the knock-on effects of static references
 * to Bonnie classes in LuceneUtils. Introduced to aid testability.
 *
 * @since 0.9.12
 */
interface LuceneIndexAccessor
{
    /**
     * Gets a Lucene {@link org.apache.lucene.index.IndexReader} at the given path.
     *
     * @param path the path.
     * @return the IndexReader.
     * @throws IndexException if there's some problem getting the reader.
     */
    IndexReader getIndexReader(String path) throws IndexException;

    /**
     * Gets a Lucene {@link org.apache.lucene.index.IndexWriter} at the given path.
     *
     * @param path the path.
     * @param create if true, then create if absent.
     * @throws IndexException if there's some problem getting the writer.
     * @return the IndexWriter.
     */
    IndexWriter getIndexWriter(String path, boolean create)  throws IndexException;
}
