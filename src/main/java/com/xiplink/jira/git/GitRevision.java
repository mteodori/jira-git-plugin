package com.xiplink.jira.git;

import java.util.Date;

public class GitRevision
{
    private long revisionNumber;
    private String message;
    private Date date;
    private String author;

    public long getRevisionNumber()
    {
        return revisionNumber;
    }

    public String getMessage()
    {
        return message;
    }

    public Date getDate()
    {
        return date;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setRevisionNumber(long revisionNumber)
    {
        this.revisionNumber = revisionNumber;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }
}
