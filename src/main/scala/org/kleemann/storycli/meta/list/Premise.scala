package org.kleemann.storycli.meta.list

import org.kleemann.storycli.meta

import scala.annotation.tailrec

object Premise {

    private def removeNewlines(s: String): String = s.replaceAll("\\r?\\n", "")

    /**
      * Designed to take the output of extractFile or ReadFile
      *
      * @param in
      * @return
      */
    def create(lines: List[String]): Either[String, meta.Premise] =  {
        @tailrec
        def loop(lines: List[String]): Either[String, meta.Premise] = {
            if (lines.isEmpty) Left("empty premise.md")
            else if (lines.head.startsWith("#")) loop(lines.tail)
            else if (lines.head.isBlank) loop(lines.tail)
            else Right(meta.Premise(removeNewlines(lines.head).trim))
        }
        loop(lines)
    }

}
