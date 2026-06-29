package org.kleemann.storycli.meta

import scala.annotation.tailrec

case class Column(label: String, value: String)

case class Incident(name: String, columns: List[Column], wordCount: Int)

case class Story(title: String, incidents: List[Incident]) {

    /**
      * Returns the Story in CSV format.
      *
      * @return Outer list has rows. Inner list has each cell of the row.
      */
    def toCsv(): List[List[String]] = {

        def escapeCsvCell(cell: String): String = {
            if (cell == null) ""
            else if (cell.contains(",") || cell.contains("\"") || cell.contains("\n"))
                // Escape existing double quotes by doubling them, then wrap the entire cell in quotes
                "\"" + cell.replace("\"", "\"\"") + "\""
            else cell
        }

        // title doesn't go in the csv content

        // traverse incidents to get all columns in an ordered list
        // we pass a set of the columns through the fold so we can easily look them up
        val (customHeadersReversed, _) = incidents.foldLeft((List[String](), Set[String]())) {
            case ((hs: List[String], hset:Set[String]), in: Incident) => {
                // traverse columns and add new ones to the headers
                val (hs2, hset2) = in.columns.foldLeft((hs, hset)) { case ((hs3, hset3), col) => {
                    val label = col.label
                    if (hset3.contains(label)) (hs3, hset3) else (label :: hs3, hset3 + label)
                }}
                (hs2, hset2)
            }
        }
        val customHeaders = customHeadersReversed.reverse

        val unescaped =
            // make first header row
            ("Chapter" :: "Incident" :: "Words" :: "Percentage" :: "Cumulative" :: customHeaders)
            ::
            // second row is the totals
            ("" :: "" :: "" :: "total" :: "100%" :: customHeaders.map{ s => ""})
            ::
            // walk down incidents to make all remaining rows
            incidents.map { in =>
                "" :: in.name :: in.wordCount.toString :: "" :: "" ::
                    customHeaders.map{ label => {
                        in.columns.find{ _.label == label } match
                            case None => ""
                            case Some(col) => col.value
                    }}
            }

        // you could probably get by with just escaping the headers and values above
        // that would be more efficient but this is bulletproof
        unescaped.map{ _.map{ escapeCsvCell(_) } }
    }
}

object Story {

    def create(lines: List[String]): Either[String, Story] =  {

        val titleRe         = """^#\s+([\w:-? ]+)\s*$""".r
        val incidentStartRe = """^<!-- begin incident:\s+([\w:-? ]+)\s*$""".r
        val columnRe        = """^Column:\s*([\w-? ]+)\s*:\s*([\w-? ]+)\s*$""".r
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
        def incidentLoop(lines: List[String], prevLineNo: Int, in: String, cs: List[Column]): Either[String, (Incident, List[String], Int)] = {
            // look for columns, end of incident, and accidental start of incident
            if (lines.isEmpty) Left(s"error(${prevLineNo}): file ended while looking for end of incident: ${in}")
            else {
                val line = lines.head
                val lineNo = prevLineNo + 1
                line match
                    case columnRe(name,value) => incidentLoop(lines.tail, lineNo, in, Column(name.trim,value.trim) :: cs)
                    case incidentEndRe() => Right(Incident(in, cs.reverse, 0), lines.tail, lineNo) // zero wordCount will be replaced by parent fn
                    case incidentStartRe(wrong) => Left(s"error(${lineNo}): second start of incident. incident: ${in}")
                    case _ => incidentLoop(lines.tail, lineNo, in, cs)
            }
        }

        @tailrec
        def loop(lines: List[String], prevLineNo:Int, title: Option[String], is: List[Incident]): Either[String, Story] = {
            // look for title and incident
            if (lines.isEmpty) {
                title match
                    case None => Left("title (# my title) is not defined in the document")
                    case Some(title) => Right(Story(title, is.reverse))
            } else {
                val line = lines.head
                val lineNo = prevLineNo + 1
                line match
                    case titleRe(latest) => title match
                        case Some(first) => Left(s"error(${lineNo}): title specified twice: 1:${first} 2:${latest}")
                        case None => loop(lines.tail, lineNo, Some(latest.trim), is)
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
        loop(lines, 0, None, Nil)
    }

    val defaultFilename = "story.md"

    def extract(repo: os.Path, filename: String = defaultFilename): Either[String, Story] =
        pipe(extractFile(repo,filename), create)

    def read(dir: os.Path, filename: String = defaultFilename): Either[String, Story] =
        pipe(readFile(dir / filename), create)

}
