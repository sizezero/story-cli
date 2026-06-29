package org.kleemann.storycli.command

import scala.annotation.tailrec

import org.kleemann.storycli.{GlobalOptions}
import org.kleemann.storycli.meta.Story

object AnalyzeCommand extends Command {

    enum Output:
        case Sc, Csv

    def parse(args: List[String]): Either[String, (String, Output)] = {

        @tailrec
        def loop(args: List[String], filename: Option[String], output: Option[Output]): Either[String, (String, Output)] = {
            if (args.isEmpty) {
                // if either option is not specified, use default values
                val fn = filename match
                    case None => Story.defaultFilename
                    case Some(f) => f
                val op = output match
                    case None => Output.Sc
                    case Some(o) => o
                Right(fn, op)
            } else {
                val arg = args.head
                arg match
                    case "-s" | "--sc" => output match
                        case Some(prev) => Left(s"output option ${prev} attempted to reset to ${arg}")
                        case None => loop(args.tail, filename, Some(Output.Sc))
                    case "-c" | "--csv" => output match
                        case Some(prev) => Left(s"output option ${prev} attempted to reset to ${arg}")
                        case None => loop(args.tail, filename, Some(Output.Csv))
                    case _ => filename match
                        case Some(prev) => Left(s"filename option ${prev} attempted to reset to ${arg}")
                        case None => loop(args.tail, Some(arg), output)
            }
        }
        loop(args, None, None)
    }



    def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match
            case Left(error) => Left(error)
            case Right(filename, output) => Story.read(os.pwd, filename) match
                case Left(error) => Left(error)
                case Right(story) =>
                    if (!os.exists(os.pwd / ".gitignore")) Left("error: current directory does not appear to be a git working copy")
                    else {
                        // target diretory must exist
                        val target = os.pwd / "target"
                        os.makeDir.all(target)
                        output match
                            case Output.Csv => {
                                val out = target / (filename.stripSuffix(".md") + ".csv")
                                os.write.over(out, story.toCsv())
                                Right(List(s"written: ${out.toString}"))
                            }
                            case Output.Sc => Left("TODO")
                    }
    }

}
