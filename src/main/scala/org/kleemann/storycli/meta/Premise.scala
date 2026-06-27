package org.kleemann.storycli.meta

case class Premise(oneLine: String)

object Premise {

    private def removeNewlines(s: String): String = s.replaceAll("\\r?\\n", "")

    /**
      * Designed to take the output of extractFile or ReadFile
      *
      * @param in
      * @return
      */
    def create(lines: List[String]): Either[String, Premise] =  {
        def loop(lines: List[String]): Either[String, Premise] = {
            if (lines.isEmpty) Left("empty premise.md")
            else if (lines.head.startsWith("#")) loop(lines.tail)
            else if (lines.head.isBlank) loop(lines.tail)
            else Right(Premise(removeNewlines(lines.head).trim))
        }
        loop(lines)
    }

    val filename = "premise.md"

    def extract(repo: os.Path): Either[String, Premise] =
        pipe(extractFile(repo,filename), create)

    def read(dir: os.Path): Either[String, Premise] =
        pipe(readFile(dir / filename), create)
}
