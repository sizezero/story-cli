package org.kleemann.storycli.command

import os.Path
import scala.annotation.tailrec

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}
import org.kleemann.storycli.meta.{Premise}

object ListCommand extends Command {

    override val commandName = "list"

    override val commandLineHelp = "story-cli list [ -p | --premise ] [ -l | --local ]"

    override val oneLineHelp = "list all stories"

    override val multiLineHelp = List(
        "The story name is shown with the directory prefix.",
        "If --premise is specified, then the one line premise from premise.md is also shown.",
        "If --local is selected then only local repos are shown as well as their git status",
        "In normal mode all displayed information comes from the remote repos",
        "In local mode all displayed information comes from the local, checked out repos",
    )

    case class Args(premise: Boolean, local: Boolean)

    // returned values are (premise: boolean, local: boolean)
    def parse(in: List[String]): Either[String, Args] = {
        @tailrec
        def loop(in: List[String], args: Args): Either[String, Args] = {
            if (in.isEmpty) Right(args)
            else in.head match {
                case "-p" | "--premise" =>
                    if (args.premise) Left("--premise specified more than once")
                    else loop(in.tail, Args(true, args.local))
                case "-l" | "--local" =>
                    if (args.local) Left("--local specified more than once")
                    else loop(in.tail, Args(args.premise, true))
                case _ => Left(s"uknown argument: ${in.head}")
            }
        }
        loop(in, Args(false,false))
    }

    private case class Dir(dir: String, premise: Option[String])

    private def remoteList(go: GlobalOptions, premise: Boolean): Either[String, List[String]] = {
        def oneLinePremise(repo: Path): String = {
            Premise.extract(repo) match
            case Left(error)    => error // embed errors in the output
            case Right(premise) => premise.oneLine
        }


                val sf = StoriesFolder(go)
                if (sf.isServer) {
                    // walk through all dirs until a directory that ends in .git
                    // we have to go one directory further since skipped files will not show up in the list
                    val customizedWalk = os.walk(
                        sf.serverStories,
                        skip = path => (path / os.up).last.endsWith(".git")
                    )
                    // get a list of dirs and premises
                    // directories of many stories end in slash
                    // story dirs may have a premise
                    val dirs: List[Dir] = customizedWalk.toList
                        .map{ path => {
                            val dir = path.subRelativeTo(sf.serverStories)
                            if (dir.ext == "git") {
                                val displayPath = dir.toString.stripSuffix(".git")
                                if (premise && path.toString.endsWith(".git"))
                                    Dir(displayPath, Some(oneLinePremise(path)))
                                else
                                    Dir(displayPath, None)
                            } else {
                                Dir(dir.toString + "/", None)
                            }
                        }}.sortBy{ _.dir }
                    val ret: List[String] =
                        // list all containing directories
                        dirs.filter{ _.dir.endsWith("/") }.map{ _.dir }
                        :::
                        // visible separator
                        "---"
                        ::
                        // list storie dirs and an optional premise
                        dirs.filter{ !_.dir.endsWith("/") }
                        .flatMap{ d =>
                            d match
                                case Dir(dir, None)          => List(dir)
                                case Dir(dir, Some(premise)) => List(dir, "    "+premise)
                        }
                    Right(ret)
                } else {
                    // this is a second example of "run this verbatum on the remote server"
                    val cmd = ("story-cli" :: go.args).mkString(" ")
                    val result = os.proc("ssh", s"${go.userName}@${go.serverName}", cmd).call()
                    if (result.exitCode == 0) Right(result.out.lines().toList)
                    else Left(result.err.text()+result.out.text())
                }
    }

    private def localList(go: GlobalOptions, premise: Boolean): Either[String, List[String]] = {

        def oneLinePremise(dir: Path): String = {
            Premise.read(dir) match
            case Left(error)    => error // embed errors in the output
            case Right(premise) => "    "+premise.oneLine
        }

        def status(dir: Path): Option[String] = {
            val result1 = os.proc("git", "status", "--porcelain").call( cwd = dir )
            val out1 = 
                if (result1.exitCode != 0)         "ERROR RUNNING GIT"
                else if (result1.out.text() == "") ""
                else                               "UNCOMMITTED"
            // git rev-list @{u}..HEAD --count
            val result2 = os.proc("git", "rev-list", "@{u}..HEAD", "--count").call( cwd = dir )
            val out2 = 
                if (result1.exitCode != 0)                 "ERROR RUNNING GIT"
                else if (result2.out.text().trim() == "0") ""
                else                                       "UNPUSHED"
            if (out1=="" && out2=="")      None
            else if (out1!="" && out2=="") Some("    !!! "+out1)
            else if (out1=="" && out2!="") Some("    !!! "+out2)
            else                           Some("    !!! "+out1+" "+out2)
        }

        // get a list of all checked out directories
        val sf = StoriesFolder(go)
        // walk through all dirs until a directory that ends in .git
        // we have to go one directory further since skipped files will not show up in the list
        val outLines = os.walk(sf.checkouts)
            .filter{ p => os.isDir(p) && p.last == ".git" }
            .flatMap{ gitFile =>
                val d = gitFile / os.up
                val l1 = d.subRelativeTo(sf.checkouts).toString :: Nil
                val l2 =
                    if (premise) oneLinePremise(d) :: l1
                    else         l1
                val l3 = status(d) match
                    case None    => l2
                    case Some(s) => s :: l2
                l3.reverse
            }
            .toList

        Right(outLines)
    }

    override def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error) => Left(error)
            case Right(Args(premise, false)) => remoteList(go, premise)
            case Right(Args(premise, true )) => localList (go, premise)
        }
    }
}
