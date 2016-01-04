package se.codeslasher.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by khazrak on 1/3/16.
 */
public class TestFileSplitter {

    @Test
    public void splitBySize() throws IOException, URISyntaxException {
        Path tempDir = Files.createTempDirectory("filesplitter");
        Path src = Paths.get(this.getClass().getClassLoader().getResource("text.txt").toURI());

        FileSplitter.splitFileBySize(src, tempDir,10, "test",false);

        long originalSize = Files.size(src);
        long actualSize = getSizeOfSplitFilesSum(tempDir);
        long count = Files.list(tempDir).count();

        Assert.assertThat(actualSize, is(equalTo(originalSize)));
        //81 bytes, 10 per split-file = 9 files
        Assert.assertThat(count, is(equalTo(9L)));

        removeDirectoryAndFiles(tempDir);
    }

    @Test
    public void splitByParts() throws IOException, URISyntaxException {
        Path tempDir = Files.createTempDirectory("filesplitter");
        Path src = Paths.get(this.getClass().getClassLoader().getResource("text.txt").toURI());

        int parts = 5;

        FileSplitter.splitFileByParts(src, tempDir,parts,"test",false);

        long originalSize = Files.size(src);
        long actualSize = getSizeOfSplitFilesSum(tempDir);
        int count = Math.toIntExact(Files.list(tempDir).count());

        Assert.assertThat(actualSize, is(equalTo(originalSize)));
        Assert.assertThat(count, is(equalTo(parts)));

        removeDirectoryAndFiles(tempDir);
    }

    private long getSizeOfSplitFilesSum(Path dest) throws IOException {
        return Files.list(dest).mapToLong(p -> {
            try {
                return Files.size(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }).sum();
    }


    /**
     * Cleans up the temporary folder
     * @param dest
     * @throws IOException
     */
    private void removeDirectoryAndFiles(Path dest) throws IOException {

        Files.list(dest).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Files.deleteIfExists(dest);

    }


}
