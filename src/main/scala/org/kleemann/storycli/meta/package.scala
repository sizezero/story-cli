package org.kleemann.storycli.meta

def extractFile(gitRepo: os.Path, filename: String): Either[String, Vector[String]] = {
    if (!os.exists(gitRepo)) Left(s"git repo dir does not exist: ${gitRepo.toString}")
    else if (!gitRepo.toString.endsWith(".git")) Left(s"git repo dir must end with \".git\": ${gitRepo.toString}")
    else {
        val result = os.proc("sh", "-c" , s"git archive --remote=${gitRepo} HEAD ${filename} | tar xO").call()
        if (result.exitCode == 0) Right(result.out.lines())
        else Left("error running git: "+result.err.text()+result.out.text())
    }
}

def readFile(file: os.Path): Either[String, Vector[String]] =
    if (os.exists(file)) Right(os.read.lines(file).toVector)
    else                 Left(s"file does not exists: ${file.toString}")

/**
  * Easily connect either extractFile or readFile to a meta file constructor such as Premise.
  * 
  * I feel like this is some known functional idiom that should have a well known name. I'm calling it pipe for now.
  *
  * @param in
  * @param f
  * @return
  */
def pipe[E,A,B](in: Either[E, A], f: A => Either[E, B]): Either[E, B] = 
    in match
        case Left(error) => Left(error)
        case Right(args) => f(args)
