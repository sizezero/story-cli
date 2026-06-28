package org.kleemann.storycli.meta

import scala.annotation.tailrec

case class Character(name: String, role: String)

object Characters {

    def create(lines: List[String]): Either[String, List[Character]] =  {

        val rolePrefix = "* Role:"
        @tailrec
        def roleLoop(lines: List[String], name: String): Character = {
            if (lines.isEmpty) Character(name, "N/A")
            else {
                val line = lines.head
                if (line.startsWith("#")) Character(name, "N/A")
                else if (line.startsWith(rolePrefix)) Character(name, line.stripPrefix(rolePrefix).trim)
                else roleLoop(lines.tail, name)
            }
        }

        val namePrefix = "## "
        @tailrec
        def characterLoop(lines: List[String], cs: List[Character]): Either[String, List[Character]] = {
            if (lines.isEmpty) Right(cs.reverse)
            else {
                val line = lines.head
                if (line.startsWith("## Template")) characterLoop(lines.tail, cs)
                else if (line.startsWith(namePrefix)) {
                    val c = roleLoop(lines.tail, line.stripPrefix(namePrefix).trim )
                    characterLoop(lines.tail, c :: cs )
                } else characterLoop(lines.tail, cs)
            }
        }
        characterLoop(lines, Nil)
    }

    val filename = "characters.md"

    def extract(repo: os.Path): Either[String, List[Character]] =
        pipe(extractFile(repo,filename), create)

    def read(dir: os.Path): Either[String, List[Character]] =
        pipe(readFile(dir / filename), create)
}
