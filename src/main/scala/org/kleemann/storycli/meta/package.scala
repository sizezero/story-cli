package org.kleemann.storycli

/**
  * This package provides all the utilities that read files from a story directoy
  * into objects that contain significant meta information from the files.
  */
package object meta {

    /**
      * A helper method that reads the contents of a file in a bare git repo into memory.
      *
      * @param gitRepo The git repo directory. It must end in ".git"
      * @param filename The filename within the repo to extract.
      * @return Either an error message or an iterator for each line of the file.
      */
    def extractFile(gitRepo: os.Path, filename: String): Either[String, Iterator[String]] = {
        if (!os.exists(gitRepo)) Left(s"git repo dir does not exist: ${gitRepo.toString}")
        else if (!gitRepo.toString.endsWith(".git")) Left(s"git repo dir must end with \".git\": ${gitRepo.toString}")
        else {
            val result = os.proc("sh", "-c" , s"git archive --remote=${gitRepo} HEAD ${filename} | tar xO").call()
            if (result.exitCode == 0)
                // lines() returns Vector[String] so we don't really get the lazy read benefit of Iterator here
                Right(result.out.lines().iterator)
            else
                Left("error running git: "+result.err.text()+result.out.text())
        }
    }

    /**
      * Reads the given file from the filesystem.
      *
      * @param file The file to read.
      * @return an iterator for each line of the file
      */
    def readFile(file: os.Path): Either[String, Iterator[String]] =
        // TODO: I'm pretty sure this reads the entire file into memory.
        // We should use os.read.lines.generator(file) but unfortunately Generator
        // only provides methods for reading the entire file, not part of it. I think
        // it is designed this way to insure that it can read and closes the resource
        // atomically.

        // TODO: we could instead change this so that it uses java methods to read the file and each
        // line and calls a passed function with the iterator.
        // This would allow it to clean up when it is done.
        if (os.exists(file)) Right(os.read.lines(file).iterator)
        else                 Left(s"file does not exists: ${file.toString}")

}

