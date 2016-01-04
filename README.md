# Chomper
A file scatter-gather (splitter and assembler)

The library can split a file into several pieces, either by size or by parts.
It can then assemble the files into the original file again, either by a List off files (orderd) or a directory (will be sorted by name).

The library reads bytes and not characters.

The checksum of the original file and assembled file will be the same (by the byte)
