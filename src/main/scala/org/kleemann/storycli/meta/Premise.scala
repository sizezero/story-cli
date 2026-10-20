package org.kleemann.storycli.meta

case class Premise(oneLine: String)

object Premise {

    val filename = "premise.md"

    private def removeNewlines(s: String): String = s.replaceAll("\\r?\\n", "")

    /**
      * Designed to take the output of extractFile or ReadFile
      *
      * @param in
      * @return
      */
    def create(lines: Iterator[String]): Either[String, Premise] =
        lines.find{ line => !line.startsWith("#") && !line.forall{ _.isWhitespace }} match
            case None => Left("empty premise.md")
            case Some(line) => Right(Premise(removeNewlines(line).trim))

    def extract(repo: os.Path): Either[String, Premise] =
        extractFile(repo, Premise.filename).flatMap{ create(_) }

    def read(dir: os.Path): Either[String, Premise] =
        readFile(dir / Premise.filename).flatMap{ create(_) }

}
