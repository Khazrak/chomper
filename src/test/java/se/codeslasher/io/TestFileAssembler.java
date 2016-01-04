package se.codeslasher.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by khazrak on 1/3/16.
 */
public class TestFileAssembler {

    @Test
    public void assembleDirectory() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        Path src = getSource();
        Path dest = getDestination();

        FileAssembler.assembleFile(src, dest, false);

        long size = Files.size(dest);
        Assert.assertThat(size, is(equalTo(81L)));

        assertChecksum(dest);

        //clean up
        Files.deleteIfExists(dest);
    }

    @Test
    public void assembleFromSources() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        Path src = getSource();
        Path dest = getDestination();

        List<Path> sources = Files.list(src).collect(Collectors.toList());
        Collections.sort(sources);

        FileAssembler.assembleFile(sources,dest, false);

        long size = Files.size(dest);
        Assert.assertThat(size, is(equalTo(81L)));

        assertChecksum(dest);

        //clean up
        Files.deleteIfExists(dest);
    }

    private void assertChecksum(Path destination) throws NoSuchAlgorithmException, IOException, URISyntaxException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte [] destinationBytes = Files.readAllBytes(destination);

        Path original = Paths.get(this.getClass().getClassLoader().getResource("text.txt").toURI());
        byte [] originalBytes = Files.readAllBytes(original);

        Assert.assertThat(Arrays.equals(originalBytes, destinationBytes), is(equalTo(true)));

        md.update(originalBytes);
        byte [] originalDigest = md.digest();

        md.reset();

        md.update(destinationBytes);
        byte [] destinationDigest = md.digest();

        Assert.assertThat(Arrays.equals(destinationDigest, originalDigest), is(equalTo(true)));

        String originalChecksum = new String(originalDigest);
        String destinationChecksum = new String(destinationDigest);

        Assert.assertThat(destinationChecksum,is(equalTo(originalChecksum)));
    }

    private Path getSource() throws URISyntaxException {
        return Paths.get(this.getClass().getClassLoader().getResource("split").toURI());
    }

    private Path getDestination() throws IOException {
        return Files.createTempFile("test","txt");
    }
}
