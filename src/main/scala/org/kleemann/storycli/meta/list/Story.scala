package org.kleemann.storycli.meta.list

import scala.annotation.tailrec

import org.kleemann.storycli.meta

object Story {

    def create(lines: List[String]): Either[String, meta.Story] =  {

        // Titles, incident names, and column labels are limited to
        // letters (upper and lowercase), numbers, underscore, hyphen,
        // question mark, and spaces. Titles may additionally contain commas.
        //
        // Column values can contain any non-space
        // character as well as spaces so pretty much everything except for tabs
        // and newlines. The latter may be too much for some downstream use.
        // We'll see.
        val titleRe         = """^title:\s+([\w:-?, ]+)\s*$""".r
        val incidentStartRe = """^<!-- begin incident:\s+([\w:-? ]+)\s*$""".r
        val columnRe        = """^Column:\s*([\w-? ]+)\s*:\s*([\S ]+)\s*$""".r
        val incidentEndRe   = """^end incident -->$""".r

        @tailrec
        def wordsLoop(lines: List[String], prevLineNo: Int, wc: Int): Either[String, (Int, List[String], Int)] = {
            if (lines.isEmpty) Right(wc, lines, prevLineNo)
            else {
                val line = lines.head
                val lineNo = prevLineNo + 1
                line match
                    case incidentStartRe(_) => Right(wc, lines, prevLineNo)
                    case incidentEndRe() => Left(s"error(${lineNo}) unexcpected incident end while in incident body")
                    case _ => {
                        val t = line.trim
                        val additionalWordCount = if (t == "") 0 else t.split("\\s+").length
                        wordsLoop(lines.tail, lineNo, wc + additionalWordCount)
                    }
            }
        }

        @tailrec
        def incidentLoop(lines: List[String], prevLineNo: Int, in: String, cs: List[meta.Column]): Either[String, (meta.Incident, List[String], Int)] = {
            // look for columns, end of incident, and accidental start of incident
            if (lines.isEmpty) Left(s"error(${prevLineNo}): file ended while looking for end of incident: ${in}")
            else {
                val line = lines.head
                val lineNo = prevLineNo + 1
                line match
                    case columnRe(name,value) => incidentLoop(lines.tail, lineNo, in, meta.Column(name.trim,value.trim) :: cs)
                    case incidentEndRe() => Right(meta.Incident(in, cs.reverse, 0), lines.tail, lineNo) // zero wordCount will be replaced by parent fn
                    case incidentStartRe(wrong) => Left(s"error(${lineNo}): second start of incident. incident: ${in}")
                    case _ => incidentLoop(lines.tail, lineNo, in, cs)
            }
        }

        @tailrec
        def loop(lines: List[String], prevLineNo:Int, title: Option[String], is: List[meta.Incident]): Either[String, meta.Story] = {
            // look for title and incident
            if (lines.isEmpty) {
                title match
                    case None => Left("title is not defined in the document")
                    case Some(title) => Right(meta.Story(title, is.reverse))
            } else {
                val line = lines.head
                val lineNo = prevLineNo + 1
                if (lineNo==2) {
                    line match
                        case titleRe(t) => loop(lines.tail, lineNo, Some(t.trim), is)
                        case _ => Left("title required on line 2")
                } else {
                    line match
                        case incidentStartRe(in) => incidentLoop(lines.tail, lineNo, in.trim, Nil) match 
                            case Left(error) => Left(error)
                            case Right(incident, nextLines, prevLineNo) => {
                                if (incident.name == "template")
                                    // ignore template incidents as well as the following word counts
                                    loop(nextLines, prevLineNo, title, is)
                                else
                                    // this is the body of text following the the incident block
                                    wordsLoop(nextLines, prevLineNo, 0) match
                                        case Left(error) => Left(error)
                                        case Right(wc, nextLines, prevLineNo) =>
                                            loop(nextLines, prevLineNo, title, incident.copy(wordCount = wc) :: is)
                            }
                        case _ => loop(lines.tail, lineNo, title, is)
                }
            }
        }
        loop(lines, 0, None, Nil)
    }

    def extract(repo: os.Path, filename: String = meta.Story.defaultFilename): Either[String, meta.Story] =
        extractFile(repo, filename).flatMap{ create(_) }

    def read(dir: os.Path, filename: String = meta.Story.defaultFilename): Either[String, meta.Story] =
        readFile(dir / filename).flatMap{ create(_) }
}