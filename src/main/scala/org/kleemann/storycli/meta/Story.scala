package org.kleemann.storycli.meta

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

    val defaultFilename = "story.md"

    // Titles, incident names, and column labels are limited to
    // letters (upper and lowercase), numbers, underscore, hyphen,
    // question mark, and spaces. Titles may additionally contain commas.
    //
    // Column values can contain any non-space
    // character as well as spaces so pretty much everything except for tabs
    // and newlines. The latter may be too much for some downstream use.
    // We'll see.
    private val titleRe         = """^title:\s+([\w:-?, ]+)\s*$""".r
    private val incidentStartRe = """^<!-- begin incident:\s+([\w:-? ]+)\s*$""".r
    private val columnRe        = """^Column:\s*([\w-? ]+)\s*:\s*([\S ]+)\s*$""".r
    private val incidentEndRe   = """^end incident -->$""".r
    private val htmlCommentRe   = """^<!--.*-->$""".r

    private case class Builder(
        prevLineNo:   Int                 = 0,
        title:        Option[String]      = None,
        incidents:    List[Incident] = Nil, // reverse order
        incidentName: Option[String]      = None,
        columns:      List[Column]   = Nil, // reverse order
        wordCount:    Option[Int]         = None
    ) {

        def add(line: String): Either[String, Builder] = {
            val lineNo = prevLineNo + 1
            def skip = Right(this.copy(prevLineNo = lineNo))
            def err(s: String) = Left(s"error(${lineNo}): ${s}")
            title match {
                case None => {
                    // we're still looking for the title
                    if (lineNo < 2)
                        skip
                    else if (lineNo == 2) {
                        line match
                            case titleRe(t) =>
                                Right(this.copy(
                                    prevLineNo = lineNo,
                                    title = Some(t.trim),
                                ))
                            case _ =>
                                err("title required on line 2")
                    } else
                        // I don't see how we can get here
                        err("title must be specified before line 2")
                }
                case Some(_) => {
                    incidentName match {
                        case None => {
                            // looking for start of the first incident block
                            line match
                                case incidentStartRe(iname) =>
                                    Right(this.copy(
                                        prevLineNo = lineNo,
                                        incidentName = Some(iname),
                                    ))
                                case incidentEndRe() =>
                                    err("expecting incident start but found end instead")
                                case _ =>
                                    skip
                        }
                        case Some(iname) => {
                            wordCount match {
                                case None => {
                                    // we're still in the incident block looking for columns or an end to the block
                                    line match
                                        case columnRe(name,value) =>
                                            Right(this.copy(
                                                prevLineNo = lineNo,
                                                columns = Column(name.trim, value.trim) :: columns, 
                                            ))
                                        case incidentEndRe() =>
                                            Right(this.copy(
                                                prevLineNo = lineNo,
                                                wordCount = Some(0),
                                            ))
                                        case incidentStartRe(wrong) =>
                                            err(s"second start of incident. incident: ${iname}")
                                        case _ =>
                                            skip
                                }
                                case Some(wc) => {
                                    // we're in the post incident text block counting words and looking for the next incident
                                    line match
                                        case incidentStartRe(nextIncidentName) => {
                                            Right(this.copy(
                                                prevLineNo = lineNo,
                                                incidents =
                                                    if (iname=="template") incidents
                                                    else
                                                        Incident(iname, columns.reverse, wc) :: incidents,
                                                incidentName = Some(nextIncidentName),
                                                columns = Nil,
                                                wordCount = None,
                                            ))
                                        }
                                        case incidentEndRe() =>
                                            Left(s"error(${lineNo}) unexpected incident end while in incident text")
                                        case htmlCommentRe() =>
                                            skip
                                        case _ => {
                                            val t = line.trim
                                            val additionalWordCount = if (t == "") 0 else t.split("\\s+").length
                                            Right(this.copy(
                                                prevLineNo = lineNo, 
                                                wordCount = Some(wc + additionalWordCount)
                                            ))
                                        }

                                }
                            }
                        }
                    }
                }
            }
        }

        def toStory: Either[String, Story] = {
            // we likely have an incomplet incident that must be completed
            def err(s: String) = Left(s"error(EOF): ${s}")
            title match {
                case None =>
                    err("title is not defined in the document")
                case Some(title) => {
                    incidentName match {
                        case None =>
                            // I'm not sure how there can be no incident name, maybe for a doc that is just a title?
                            Right(Story(title, incidents.reverse))
                        case Some(iname) => {
                            // we can get a None word count if an end incident is the last line of the file
                            wordCount match {
                                case None =>
                                    err(s"file ended while looking for end of incident: ${iname}")
                                case Some(wc) => {
                                    Right(Story(
                                        title, 
                                        if (iname=="template")
                                            incidents.reverse
                                        else
                                            (Incident(iname, columns.reverse, wc) :: incidents).reverse
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // This is a foldLeft but breaks out when the returned Either value is the Left argument
    // the signature is foldLeft with the returned B replaced with Either[E, B] where E is the 
    // error type often a String.
    private def foldLeftWhileRight[A, B, E](it: Iterator[A], z: B)(op: (B, A) => Either[E, B]): Either[E, B] = {
        // since we are consuming from a mutable iterator
        // I'm not sure if it even makes sense to do this functionally
        var acc: Either[E, B] = Right(z)
        var keepRunning = true
        while (keepRunning && it.hasNext)
            keepRunning = acc match
                case Left(_)  => false
                case Right(b) => {
                    acc = op(b, it.next())
                    true
                }
        acc
    }

    def create(lines: Iterator[String]): Either[String, Story] =
        foldLeftWhileRight(lines, Builder()) {
            (builder, line) => builder.add(line)
        }.flatMap{ _.toStory }

    def extract(repo: os.Path, filename: String = Story.defaultFilename): Either[String, Story] =
        extractFile(repo, filename).flatMap{ create(_) }

    def read(dir: os.Path, filename: String = Story.defaultFilename): Either[String, Story] =
        readFile(dir / filename).flatMap{ create(_) }

}
