import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import java.io.File;
import java.util.Map;

/**
 * @author Ivan Sungurov
 */
public class RepoTest {
    public static void main(String[] args) throws Exception {
        String path = "/home/ivan/issart/charm.git";
        File root = new File(path);
        RepositoryBuilder builder = new RepositoryBuilder().addCeilingDirectory(root).findGitDir(root);
        if(builder.getGitDir() == null) {
            builder.setGitDir(root);
        }

        Repository repository = builder.build();
        System.out.println(repository.getObjectsDirectory().exists());

        repository.scanForRepoChanges();
        for(Map.Entry<String, Ref> pair : repository.getAllRefs().entrySet()) {
            System.out.println(pair.getKey());
        }

        System.out.println(repository.getRef("HEAD"));
    }
}
