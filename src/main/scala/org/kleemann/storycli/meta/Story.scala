package org.kleemann.storycli.meta

import scala.annotation.tailrec

case class Column(label: String, value: String)

case class Incident(name: String, columns: List[Column], wordCount: Int)

case class Story(title: String, incidents: List[Incident]) {

    private def getCustomHeaders(): List[String] = {
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
        customHeadersReversed.reverse
    }

    /**
      * Returns the Story in CSV format.
      *
      * @return Outer list has rows. Inner list has each cell of the row.
      */
    def toCsv(): List[String] = {

        // title doesn't go in the csv content

        val customHeaders = getCustomHeaders()

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

        def escapeCsvCell(cell: String): String = {
            if (cell == null) ""
            else if (cell.contains(",") || cell.contains("\"") || cell.contains("\n"))
                // Escape existing double quotes by doubling them, then wrap the entire cell in quotes
                "\"" + cell.replace("\"", "\"\"") + "\""
            else cell
        }

        // you could probably get by with just escaping the headers and values above
        // that would be more efficient but this is bulletproof
        unescaped.map{ _.map{ escapeCsvCell(_) }.mkString(",")+"\n" }
    }

    def toSc(): List[String] = {

        val customHeaders = getCustomHeaders()

        // matrix builder class lets us easily add labels and formulas
        // while keeping track of columns and rows
        case class MatrixBuilder(row: Int, col: Int, acc: List[String]) {
            def add(s: String)    = MatrixBuilder(row,   col,   s :: acc)
            def addInc(s: String) = MatrixBuilder(row,   col+1, s :: acc)
            def blank             = MatrixBuilder(row,   col+1,      acc)
            def cr                = MatrixBuilder(row+1,     0,      acc)
            // TODO: we blow up with more than 26 columns but that won't happen
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" // safest scala method of itoa
            def colText = alphabet(col)
            def loc = s"${colText}${row}"
            def lab(value: String, align: String = "") = {
                val cmd = align match
                    case "left" => "leftstring"
                    case _      => "label"
                addInc(s"${cmd} ${loc} = \""+value.replaceAll("\"", "\\\"")+"\"")
            }
            def frm(value: String) =
                addInc(s"let ${loc} = ${value}")
            def frm(value: String, format: String) = {
                add(s"let ${loc} = ${value}").
                addInc(s"fmt ${loc} \"${format}\"")
            }
            // This allows code to be inserted into MatrixBuilder construction.
            // I'm not sure if this is the right idiom.
            def code(fn: MatrixBuilder => MatrixBuilder): MatrixBuilder = fn(this)
            def toList = acc.reverse
        }

        val minIncidentNameWidth = 6
        val incidentValueWidth = 16

        MatrixBuilder(0, 0, Nil).
            // magic starting header values
            add("""newsheet "Sheet1"""").
            add("""movetosheet "Sheet1"""").
            add("offscr_sc_cols 0").
            add("offscr_sc_rows 0").
            add("nb_frozen_rows 0").
            add("nb_frozen_cols 0").
            add("nb_frozen_screenrows 0").
            add("format A 6 2 0"). // format Chp as narrow as possible
            code{ mb => {
                val maxWidth = incidents.foldLeft(minIncidentNameWidth) { case (maxWidth, incident) =>
                    scala.math.max(maxWidth, incident.name.length)
                }
                mb.add(s"format B ${maxWidth} 0 0")
            }}.
            add("format C 8 0 0"). // format wordcount  column: narrower, no decimal places
            add("format D 6 0 0"). // format percentage column: narrower, no decimal places
            add("format E 6 0 0"). // format cumulative column: narrower, no decimal places
            // incident columns are a little wider
            code{ mb =>
                (5 to (5+customHeaders.length)).foldLeft(mb){
                    case (mb, i) => mb.add(s"format ${mb.alphabet(i)} ${incidentValueWidth} 0 0")
                }
            }.
            code{ mb =>
                incidents.foldLeft((mb,2)){ case ((mb, row), incident) =>
                    // the maximum character length of all the columns used by this incident
                    val maxLength = incident.columns.foldLeft(0){ case (max, column) => scala.math.max(max, column.value.length) }
                    val height = maxLength / incidentValueWidth + 1
                    if (height > 1)
                        (mb.add(s"format ${row} ${height}"), row+1)
                    else
                        (mb                                , row+1)
                }._1
            }.
            add("freeze B"). // freeze Incident column
            add("freeze 0"). // freeze header row
            // first header row
            lab("Chp").lab("Incident").lab("Words").lab("Pct").lab("Cum").
            // list of custom headers into labels
            code{ mb => customHeaders.foldLeft(mb){ case (mb, s) => mb.lab(s)}}.
            cr.
            // second row is the totals
            blank.
            blank.
            frm(s"@sum({\"Sheet1\"}!C2:{\"Sheet1\"}!C${2+incidents.length})").
            frm(s"@sum({\"Sheet1\"}!D2:{\"Sheet1\"}!D${2+incidents.length})", "#%").
            blank.
            // for each custom header, add an empty label
            code{ mb => customHeaders.foldLeft(mb){ case (mb, s) => mb.blank }}.
            cr.
            // walk down incidents to make all remaining rows
            code{ mb => incidents.foldLeft(mb){ case (mb2, in) =>
                mb2.blank.
                lab(in.name,"left").
                frm(in.wordCount.toString).
                frm("{\"Sheet1\"}!C"+mb2.row+"/{\"Sheet1\"}!C$1", "#%").
                frm(s"@sum({\"Sheet1\"}!D2:{\"Sheet1\"}!D${mb2.row})", "#%").
                code{ mb3 =>
                    customHeaders.foldLeft(mb3){ case (mb4, label) =>
                        in.columns.find{ _.label == label } match
                            case None      => mb4.blank
                            case Some(col) => mb4.lab(col.value, "left")
                }}.cr
            }}.
            // wrap up
            add("goto A0").
            toList.
            map{ _+"\n" }
    }
}

object Story {

    def create(lines: List[String]): Either[String, Story] =  {

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
                    case None => Left("title is not defined in the document")
                    case Some(title) => Right(Story(title, is.reverse))
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

    val defaultFilename = "story.md"

    def extract(repo: os.Path, filename: String = defaultFilename): Either[String, Story] =
        pipe(extractFile(repo,filename), create)

    def read(dir: os.Path, filename: String = defaultFilename): Either[String, Story] =
        pipe(readFile(dir / filename), create)

}
