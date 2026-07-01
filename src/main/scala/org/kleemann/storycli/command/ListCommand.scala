package org.kleemann.storycli.command

import os.Path
import scala.annotation.tailrec

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}
import org.kleemann.storycli.meta.{Premise}

object ListCommand extends Command {

    override val commandName = "list"

    override val commandLineHelp = "story-cli list [ -p | --premise ]"

    override val oneLineHelp = "list all stories on the server"

    override val multiLineHelp = List(
        "The story name is shown with the directory prefix.",
        "If --premise is specified, then the one line premise from premise.md is also shown.",
        "All displayed information is from the server story repos.",
    )

    // returned booleans are (json, premise)
    def parse(args: List[String]): Either[String, Boolean] = {
        @tailrec
        def loop(args: List[String], premise: Boolean): Either[String, Boolean] = {
            if (args.isEmpty) Right(premise)
            else args.head match {
                case "-p" | "--premise" =>
                    if (premise) Left("--premise specified more than once")
                    else loop(args.tail, true)
                case _ => Left(s"uknown argument: ${args.head}")
            }
        }
        loop(args, false)
    }

    /**
      * attempts to read the file "premise.md" from the root of the passed git repo.
      * The one line premise at the top of the page is returned.
      * If any errors occur, they are returned as the String.
      *
      * @param repo
      * @return
      */
    def oneLinePremise(repo: Path): String =
        Premise.extract(repo) match
            case Left(error)    => error // embed errors in the output
            case Right(premise) => premise.oneLine

    override def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error) => Left(error)
            case Right(premise) => {
                val sf = StoriesFolder(go)
                if (sf.isServer) {
                    // walk through all dirs until a directory that ends in .git
                    // we have to go one directory further since skipped files will not show up in the list
                    val customizedWalk = os.walk(
                        sf.serverStories,
                        skip = path => (path / os.up).last.endsWith(".git")
                    )
                    val ret = customizedWalk.toList.sorted
                        .flatMap{ path => {
                            val displayPath = path.subRelativeTo(sf.serverStories).toString.stripSuffix(".git")
                            if (premise && path.toString.endsWith(".git"))
                                List(displayPath, "    "+oneLinePremise(path))
                            else
                                List(displayPath)
                        }}
                    Right(ret)
                } else {
                    // this is a second example of "run this verbatum on the remote server"
                    val cmd = ("story-cli" :: go.args).mkString(" ")
                    val result = os.proc("ssh", s"${go.userName}@${go.serverName}", cmd).call()
                    if (result.exitCode == 0) Right(result.out.lines().toList)
                    else Left(result.err.text()+result.out.text())
                }
            }
        }
    }
}
