package org.kleemann.storycli.meta.iterator

import org.kleemann.storycli.meta

object Premise {

    private def removeNewlines(s: String): String = s.replaceAll("\\r?\\n", "")

    /**
      * Designed to take the output of extractFile or ReadFile
      *
      * @param in
      * @return
      */
    def create(lines: Iterator[String]): Either[String, meta.Premise] =
        lines.find{ line => !line.startsWith("#") && !line.forall{ _.isWhitespace }} match
            case None => Left("empty premise.md")
            case Some(line) => Right(meta.Premise(removeNewlines(line).trim))

    def extract(repo: os.Path): Either[String, meta.Premise] =
        meta.pipe(extractFile(repo, meta.Premise.filename), create)

    def read(dir: os.Path): Either[String, meta.Premise] =
        meta.pipe(readFile(dir / meta.Premise.filename), create)
}

