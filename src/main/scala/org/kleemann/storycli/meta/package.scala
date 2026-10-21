package org.kleemann.storycli

package object meta {
  
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

    def readFile(file: os.Path): Either[String, Iterator[String]] =
        // TODO: I'm pretty sure this reads the entire file into memory.
        // We should use os.read.lines.generator(file) but unfortunately Generator
        // only provides methods for reading the entire file, not part of it. I think
        // it is designed this way to insure that it can read and closes the resource
        // atomically.
        if (os.exists(file)) Right(os.read.lines(file).iterator)
        else                 Left(s"file does not exists: ${file.toString}")

}

