package com.xiplink.jira.git.revisions;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * An implementation that uses JIRA's LuceneUtils for its guts.
 *
 * @since 0.9.12
 */
class DefaultLuceneIndexAccessor implements LuceneIndexAccessor
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultLuceneIndexAccessor.class);

    public IndexReader getIndexReader(String path) throws IOException
    {
        return IndexReader.open(getDirectory(path));
    }
    
    private Directory getDirectory(String path) throws IOException
    {
        return FSDirectory.open(new File(path), new SimpleFSLockFactory());
    }

    public IndexWriter getIndexWriter(String path, boolean create, Analyzer analyzer) throws IOException
    {
        // Everything in this method copied from LuceneUtils
        try
       {
           createDirRobust(path);

           final IndexWriter indexWriter = new IndexWriter(getDirectory(path), analyzer, create, IndexWriter.MaxFieldLength.LIMITED);
           indexWriter.setUseCompoundFile(true);
           return indexWriter;
       }
       catch (final IOException e)
       {
           LOG.error("Problem with path " + path + ": " + e.getMessage(), e);
           throw new IOException("Problem with path " + path + ": " + e.getMessage(), e);
       }
    }

    /**
     * Create a directory (robustly) or throw appropriate Exception
     *
     * @param path Lucene index directory path
     * @throws IOException if cannot create directory, write to the directory, or not a directory
     */
   private static void createDirRobust(final String path) throws IOException
   {
       final File potentialPath = new File(path);
       if (!potentialPath.exists())
       {
           LOG.warn("Directory " + path + " does not exist - perhaps it was deleted?  Creating..");

           final boolean created = potentialPath.mkdirs();
           if (!created)
           {
               LOG.warn("Directory " + path + " could not be created.  Aborting index creation");
               throw new IOException("Could not create directory: " + path);
           }
       }
       if (!potentialPath.isDirectory())
       {
           LOG.warn("File " + path + " is not a directory.  Cannot create index");
           throw new IOException("File " + path + " is not a directory.  Cannot create index");
       }
       if (!potentialPath.canWrite())
       {
           LOG.warn("Dir " + path + " is not writable.  Cannot create index");
           throw new IOException("Dir " + path + " is not writable.  Cannot create index");
       }
   }
}
