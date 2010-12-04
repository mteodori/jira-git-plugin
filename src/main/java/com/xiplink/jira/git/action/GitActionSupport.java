package com.xiplink.jira.git.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.xiplink.jira.git.MultipleGitRepositoryManager;
import com.xiplink.jira.git.WebLinkType;

/**
 * Base class for the Git plugins actions.
 */
public class GitActionSupport extends JiraWebActionSupport {

    private MultipleGitRepositoryManager multipleRepoManager;
    private List<WebLinkType> webLinkTypes;

    public GitActionSupport(MultipleGitRepositoryManager manager) {
        this.multipleRepoManager = manager;
    }

    protected MultipleGitRepositoryManager getMultipleRepoManager() {
        return multipleRepoManager;
    }

    public boolean hasPermissions() {
        return isHasPermission(Permissions.ADMINISTER);
    }

    public String doDefault() {
        if (!hasPermissions()) {
            addErrorMessage(getText("git.admin.privilege.required"));
            return PERMISSION_VIOLATION_RESULT;
        }

        return INPUT;
    }

    public List<WebLinkType> getWebLinkTypes() throws IOException {
        if (webLinkTypes == null) {
            webLinkTypes = new ArrayList<WebLinkType>();
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/gitweblinktypes.properties"));

            String[] types = properties.getProperty("types", "").split(" ");
            for (String type : types) {
                webLinkTypes.add(new WebLinkType(type,
                        properties.getProperty(type + ".name", type),
                        properties.getProperty(type + ".view"),
                        properties.getProperty(type + ".changeset"),
                        properties.getProperty(type + ".file.added"),
                        properties.getProperty(type + ".file.modified"),
                        properties.getProperty(type + ".file.deleted")));
            }
        }
        return webLinkTypes;
    }
}
