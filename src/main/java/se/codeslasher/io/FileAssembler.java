package se.codeslasher.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assembles files from a List of sources (Path)
 */
public class FileAssembler {

    private static Logger logger;
    private static final int BLOCK_SIZE;

    static {
        logger = LoggerFactory.getLogger(FileAssembler.class);
        BLOCK_SIZE = 8192;
        logger.info("Using block size {}", BLOCK_SIZE);
    }

    /**
     * Assembles a file from a directory. It will use all the files in the directory
     * @param sourceDirectory - The directory to assemble files from
     * @param destination - The destination file
     * @param deleteSources - If the source should be deleted after assemble
     * @return
     * @throws IOException
     */
    public static Path assembleFile(Path sourceDirectory, Path destination, boolean deleteSources) throws IOException {
        validateSourceDirectory(sourceDirectory);
        List <Path> files = Files.list(sourceDirectory).collect(Collectors.toList());
        Collections.sort(files); //It does not take them in order if you don't sort them

        Path result = assembleFile(files, destination, deleteSources);

        if(deleteSources) {
            Files.deleteIfExists(sourceDirectory);
        }

        return result;
    }

    /**
     *
     * @param sources - A list of files (in order) to use for the assembling of a file
     * @param destination - The destination file
     * @param deleteSources - If the source should be deleted after assemble
     * @return
     * @throws IOException
     */
    public static Path assembleFile(List<Path> sources, Path destination, boolean deleteSources) throws IOException {
        Files.deleteIfExists(destination);
        Files.createFile(destination);
        validateDestination(destination);
        validateSources(sources);

        logger.debug("Assembling file to: {}", destination.toAbsolutePath());

        try(OutputStream output = new BufferedOutputStream(Files.newOutputStream(destination, StandardOpenOption.WRITE))) {
            for(Path p : sources) {
                long size = Files.size(p);
                logger.trace("Assemble part {}, size {}", p.toAbsolutePath(), size);
                writeSourceToDestination(p, output);
            }
        }

        if(deleteSources) {
            logger.debug("Deleting sources");
            deleteSources(sources);
        }

        return destination;
    }

    /**
     * Writes a split-file to the assembling file
     * @param source - The split file
     * @param output - The output (destination)
     * @throws IOException
     */
    private static void writeSourceToDestination(Path source, OutputStream output) throws IOException {
        byte [] bytes = new byte[BLOCK_SIZE];
        byte [] remainingBytes;
        long size = Files.size(source);
        logger.trace("Assemble part {}, size {}", source.toAbsolutePath(), size);
        try(InputStream input = new BufferedInputStream(Files.newInputStream(source, StandardOpenOption.READ))) {
            while(size > BLOCK_SIZE) {
                size -= input.read(bytes);
                output.write(bytes);
            }

            if(size > 0) {
                remainingBytes = new byte[Math.toIntExact(size)];
                input.read(remainingBytes);
                output.write(remainingBytes);
            }
        }
    }

    private static void deleteSources(List<Path> sources) throws IOException {
        for(Path p : sources) {
            Files.deleteIfExists(p);
        }
    }

    private static void validateDestination(Path path) throws IOException {
        if(!Files.isWritable(path)) {
            throw new IOException("Destination is not writable: "+path.toAbsolutePath().toString());
        }
        if(Files.isDirectory(path)) {
            throw new IOException("Destination is a directory: "+path.toAbsolutePath().toString());
        }
    }

    private static void validateSources(List<Path> sources) {
        sources.stream().forEach(p -> {
            try {
                validateSource(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void validateSource(Path path) throws IOException {
        if(!Files.exists(path)) {
            throw new IOException("Source doesn't exist: "+path.toAbsolutePath().toString());
        }
        if(!Files.isReadable(path)) {
            throw new IOException("Source not readable: "+path.toAbsolutePath().toString());
        }
    }

    private static void validateSourceDirectory(Path path) throws IOException {
        validateSource(path);
        if(!Files.isDirectory(path)) {
            throw new IOException("Source is not a directory");
        }
        if(Files.list(path).count() == 0) {
            throw new IOException("No files in directory");
        }
    }
}
