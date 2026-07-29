package org.kleemann.storycli.meta

/**
  * Easily connect either extractFile or readFile to a meta file constructor such as Premise.create().
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
