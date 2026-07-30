package org.kleemann.storycli.meta

/**
  * This package contains the original version of the code that parsed various files. 
  * The input is a List[String] which represents the fully loaded lines of the file.
  * The parsing was performed with tail recurive functions that used the typical
  * head/tail mechanism. I found that I could handle heierarchical state by passing 
  * control to a different function and then returning to the parent function. The child 
  * function only needed a subset of the state that was relavant to it.
  * 
  * When I realized that one of the files to read is my story draft, I felt uncomfortable 
  * with a parser implementation that required the entire contents of the file to be read into
  * memory. Even the the functions only cared about a line at a time, being passed a ref that 
  * can be broken into head and tail means there are few compile time options that verify that 
  * the function is only reading one line at a time.
  * 
  * I was intrigued by using Streams. This is a model that lazily reads from a file and keeps a 
  * list of the contents of the file but will garbage collect it if there are no reference to it.
  * Best practice says to read this with a tail recursive function so that previous heads go out of 
  * scope and can be garbage collected. The problem is that this constraint can't be checked at compile 
  * time. The only way to be certain that this is happening is to perform a run-time memory profile.
  * Even then, there's always a chance that the program run ends before there is any garbage collection.
  * 
  * Stream was eventually deprecated for lazy lists. Most people say that the safest way to read a file is via 
  * iterators. The negative of iterators is that you can't use the head/tail functional paradigm but instead 
  * have to shove a builder object through foldRight.
  * 
  */
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

