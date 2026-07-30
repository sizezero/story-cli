package org.kleemann.storycli.meta.list

import scala.annotation.tailrec

import org.kleemann.storycli.meta

object Characters {

    def create(lines: List[String]): Either[String, List[meta.Character]] =  {

        val rolePrefix = "* Role:"
        @tailrec
        def roleLoop(lines: List[String], name: String): meta.Character = {
            if (lines.isEmpty) meta.Character(name, "N/A")
            else {
                val line = lines.head
                if (line.startsWith("#")) meta.Character(name, "N/A")
                else if (line.startsWith(rolePrefix)) meta.Character(name, line.stripPrefix(rolePrefix).trim)
                else roleLoop(lines.tail, name)
            }
        }

        val namePrefix = "## "
        @tailrec
        def characterLoop(lines: List[String], cs: List[meta.Character]): Either[String, List[meta.Character]] = {
            if (lines.isEmpty) Right(cs.reverse)
            else {
                val line = lines.head
                if (line.startsWith(namePrefix)) {
                    if (line.startsWith("## Template")) characterLoop(lines.tail, cs)
                    else {
                        val c = roleLoop(lines.tail, line.stripPrefix(namePrefix).trim )
                        characterLoop(lines.tail, c :: cs )
                    }
                } else characterLoop(lines.tail, cs)
            }
        }
        characterLoop(lines, Nil)
    }

    def extract(repo: os.Path): Either[String, List[meta.Character]] =
        extractFile(repo, meta.Characters.filename).flatMap{ create(_) }

    def read(dir: os.Path): Either[String, List[meta.Character]] =
        readFile(dir / meta.Characters.filename).flatMap{ create(_) }
}