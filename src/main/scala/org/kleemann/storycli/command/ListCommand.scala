package org.kleemann.storycli.command

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}
import os.Path

object ListCommand extends Command {

    // returned booleans are (json, premise)
    def parse(args: List[String]): Either[String, (Boolean, Boolean)] = {

        def loop(args: List[String], json: Boolean, premise: Boolean): Either[String, (Boolean, Boolean)] = {
            if (args.isEmpty) Right(json, premise)
            else args.head match {
                case "-j" | "--json" =>
                    if (json) Left("--json specified more than once")
                    else loop(args.tail, true, premise)
                case "-p" | "--premise" =>
                    if (premise) Left("--premise specified more than once")
                    else loop(args.tail, json, true)
                case _ => Left(s"uknown argument: ${args.head}")
            }
        }
        loop(args, false, false)
    }

    def removeNewlines(s: String): String = s.replaceAll("\\r?\\n", "")

    /**
      * attempts to read the file "premise.md" from the root of the passed git repo.
      * The one line premise at the top of the page is returned.
      * If any errors occur, they are returned as the String.
      *
      * @param repo
      * @return
      */
    def oneLinePremise(repo: Path): String = {
        // git archive --remote=file:///home/robert/stories/development/repos/template.git HEAD premise.md | tar xO
        val result = os.proc("sh", "-c" , s"git archive --remote=${repo} HEAD premise.md | tar xO").call()
        if (result.exitCode == 0) {
            def loop(lines: Vector[String]): String = {
                if (lines.isEmpty) "empty premise.md"
                else if (lines.head.startsWith("#")) loop(lines.tail)
                else if (lines.head.isBlank) loop(lines.tail)
                else removeNewlines(lines.head).trim
            }
            loop(result.out.lines())
        }
        else removeNewlines(result.err.text()+result.out.text()).trim
    }
    def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error) => Left(error)
            case Right(json, premise) => {
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
