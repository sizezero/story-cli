package org.kleemann.storycli.meta

package object list {
  
    def extractFile(gitRepo: os.Path, filename: String): Either[String, List[String]] = {
        if (!os.exists(gitRepo)) Left(s"git repo dir does not exist: ${gitRepo.toString}")
        else if (!gitRepo.toString.endsWith(".git")) Left(s"git repo dir must end with \".git\": ${gitRepo.toString}")
        else {
            val result = os.proc("sh", "-c" , s"git archive --remote=${gitRepo} HEAD ${filename} | tar xO").call()
            if (result.exitCode == 0) Right(result.out.lines().toList)
            else Left("error running git: "+result.err.text()+result.out.text())
        }
    }

    def readFile(file: os.Path): Either[String, List[String]] =
        if (os.exists(file)) Right(os.read.lines(file).toList)
        else                 Left(s"file does not exists: ${file.toString}")

}

