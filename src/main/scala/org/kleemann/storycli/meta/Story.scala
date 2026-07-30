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

    def extract(repo: os.Path, filename: String = defaultFilename): Either[String, Story] =
        stream.Story.extract(repo, filename)

    def read(dir: os.Path, filename: String = defaultFilename): Either[String, Story] =
        stream.Story.read(dir, filename)

    // for testing
    def create(lines: List[String]): Either[String, Story] =
        stream.Story.create(lines.iterator)

}
