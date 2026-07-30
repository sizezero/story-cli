package org.kleemann.storycli.meta.iterator

import org.kleemann.storycli.meta
import scala.annotation.tailrec

object Story {

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
        incidents:    List[meta.Incident] = Nil, // reverse order
        incidentName: Option[String]      = None,
        columns:      List[meta.Column]   = Nil, // reverse order
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
                                                columns = meta.Column(name.trim, value.trim) :: columns, 
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
                                                        meta.Incident(iname, columns.reverse, wc) :: incidents,
                                                incidentName = Some(nextIncidentName),
                                                columns = Nil,
                                                wordCount = None,
                                            ))
                                        }
                                        case incidentEndRe() =>
                                            Left(s"error(${lineNo}) unexcpected incident end while in incident text")
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

        def toStory: Either[String, meta.Story] = {
            // we likely have an incomplet incident that must be completed
            def err(s: String) = Left(s"error(EOF): ${s}")
            title match {
                case None =>
                    err("title is not defined in the document")
                case Some(title) => {
                    incidentName match {
                        case None =>
                            // I'm not sure how there can be no incident name, maybe for a doc that is just a title?
                            Right(meta.Story(title, incidents.reverse))
                        case Some(iname) => {
                            // we can get a None word count if an end incident is the last line of the file
                            wordCount match {
                                case None =>
                                    err(s"file ended while looking for end of incident: ${iname}")
                                case Some(wc) => {
                                    Right(meta.Story(
                                        title, 
                                        if (iname=="template")
                                            incidents
                                        else
                                            (meta.Incident(iname, columns.reverse, wc) :: incidents).reverse
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
    // the signature is foldLeft with the returned B replaced with Either[E, B]
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

    def create(lines: Iterator[String]): Either[String, meta.Story] =
        foldLeftWhileRight(lines, Builder()) {
            (builder, line) => builder.add(line)
        }.flatMap{ _.toStory }

    def extract(repo: os.Path, filename: String = meta.Story.defaultFilename): Either[String, meta.Story] =
        extractFile(repo, filename).flatMap{ create(_) }

    def read(dir: os.Path, filename: String = meta.Story.defaultFilename): Either[String, meta.Story] =
        readFile(dir / filename).flatMap{ create(_) }
}