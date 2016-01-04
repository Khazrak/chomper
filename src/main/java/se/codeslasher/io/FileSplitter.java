package se.codeslasher.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Splits a file into smaller files. Uses Path (Java NIO)
 *
 */
public class FileSplitter {

    private static Logger logger;
    private static final int BLOCK_SIZE;

    static {
        logger = LoggerFactory.getLogger(FileAssembler.class);
        BLOCK_SIZE = 8192;
        logger.info("Using block size {}", BLOCK_SIZE);
    }

    /**
     * Splits a file into smaller parts based on number of parts and optionally deletes the source file after it's finished.
     *
     * @param src - The Source file
     * @param dest - The Destination Directory
     * @param parts - How many parts to split the file in
     * @param baseName - Base name for the smaller files
     * @param deleteSrc - If the source file should be deleted after splitting it
     * @throws IOException
     */
    public static void splitFileByParts(Path src, Path dest, int parts, String baseName, boolean deleteSrc) throws IOException {
        validateSrc(src);

        long size = Files.size(src);
        long sizePerPart = size / parts;

        if(size % parts != 0) {
            sizePerPart++;
        }

        splitFileBySize(src,dest,sizePerPart,baseName,deleteSrc);
    }

    /**
     * Splits a file into smaller parts based on size and optionally deletes the source file after it's finished.
     * @param src - The Source file
     * @param dest - The Destination Directory
     * @param bytesPerFile - How many bytes a part should be
     * @param baseName - Base name for the smaller files
     * @param deleteSrc - If the source file should be deleted after splitting it
     * @throws IOException
     */
    public static void splitFileBySize(Path src, Path dest, long bytesPerFile, String baseName, boolean deleteSrc) throws IOException {
        validateSrc(src);
        validateDest(dest);

        long size = Files.size(src);
        long parts = size / bytesPerFile;

        if(size % bytesPerFile != 0) {
            parts++;
        }

        logger.debug("Splitting file {}, size (bytes) {}, bytes per file {}", src.toAbsolutePath(), size, bytesPerFile);

        try(InputStream input = new BufferedInputStream(Files.newInputStream(src, StandardOpenOption.READ),BLOCK_SIZE)) {
            for(long i = 0; i < parts; i++) {
                Path next = Paths.get(dest.toString(),baseName +"-" +i+".split");
                Files.deleteIfExists(next);
                Files.createFile(next);

                logger.trace("Creating split-file {}", next.toAbsolutePath());

                size = writeSplit(next, input, bytesPerFile, size);
            }
        }

        if(deleteSrc) {
            Files.deleteIfExists(src);
        }
    }

    /**
     *
     * @param next - Path of the next split-file
     * @param input - InputStream of source
     * @param size - Remaining size of source
     * @param bytesPerFile - How many bytes a file should aim for
     * @return
     * @throws IOException
     */
    private static long writeSplit(Path next, InputStream input, long bytesPerFile, long size) throws IOException {
        try(OutputStream output = new BufferedOutputStream(Files.newOutputStream(next, StandardOpenOption.WRITE))) {
            if(bytesPerFile < size) {
                size = writeRegularSplitFile(input, output,size,bytesPerFile);
            }
            else {
                size = writeLastSplitFile(input, output, size, bytesPerFile);
            }
        }
        return size;
    }

    /**
     * Writes a split-file to the destination folder
     * @param input - InputStream of source
     * @param output - OutputStream of destination split-file
     * @param size - Remaining size of source
     * @param bytesPerFile - How many bytes a file should aim for
     * @return
     * @throws IOException
     */
    private static long writeRegularSplitFile(InputStream input, OutputStream output, long size, long bytesPerFile) throws IOException {
        long it = bytesPerFile / BLOCK_SIZE;
        byte [] bytes = new byte [BLOCK_SIZE];
        for(int y = 0; y< it;y++) {
            size -= input.read(bytes);
            output.write(bytes);
        }
        long bytesLeft = bytesPerFile % BLOCK_SIZE;

        size = writeLastSplitPart(input, output, size, bytesLeft);

        return size;
    }

    /**
     * Writes the last split-file that usually have a different size then the others
     * @param input - InputStream of source
     * @param output - OutputStream of destination split-file
     * @param size - Remaining size of source
     * @param bytesPerFile - How many bytes a file should aim for
     * @return
     * @throws IOException
     */
    private static long writeLastSplitFile(InputStream input, OutputStream output, long size, long bytesPerFile) throws IOException {
        byte [] bytes = new byte [BLOCK_SIZE];;
        while(size > BLOCK_SIZE) {
            size -= input.read(bytes);
            output.write(bytes);
        }
        size = writeLastSplitPart(input, output, size, size);

        return size;
    }

    /**
     * Writes the last bytes of a split-file
     * @param input - InputStream of source
     * @param output - OutputStream of destination split-file
     * @param size - Remaining size of source
     * @param bytesLeft - Remaining bytes of that split-file
     * @return
     * @throws IOException
     */
    private static long writeLastSplitPart(InputStream input, OutputStream output, long size, long bytesLeft) throws IOException {
        if(bytesLeft > 0) {
            byte [] bytes = new byte[Math.toIntExact(bytesLeft)];
            size -= input.read(bytes);
            output.write(bytes);
        }
        return size;
    }

    /**
     * Validates that the source file exists and is readable
     * @param src - The source file
     * @throws IOException
     */
    private static void validateSrc(Path src) throws IOException {
        if(!Files.exists(src)) {
            throw new FileNotFoundException("Source file does not exist: "+src.toAbsolutePath().toString());
        } else if(!Files.isReadable(src)) {
            throw new IOException("Source file not readable");
        } else if(Files.isDirectory(src)) {
            throw new IOException("Source file is a directory");
        }
    }

    /**
     * Validates that the destination is a directory and is writable
     * It will try to create the directory if it doesn't exist
     * @param dest - Path to destination directory
     * @throws IOException
     */
    private static void validateDest(Path dest) throws IOException {
        if(!Files.exists(dest) && Files.isWritable(dest) && Files.isDirectory(dest)) {
            Files.createDirectories(dest);
        }
        else if(!Files.isDirectory(dest)) {
            throw new IOException("Destination is not a directory");
        }

        if(!Files.isWritable(dest)) {
            throw new IOException("Destination is not writable");
        }
    }

}
