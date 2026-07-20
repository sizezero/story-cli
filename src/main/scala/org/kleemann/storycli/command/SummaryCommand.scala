package org.kleemann.storycli.command

import scala.annotation.tailrec

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}
import org.kleemann.storycli.meta.{Character, Characters, Premise}

object SummaryCommand extends Command {

    override val commandName = "summary"

    override val commandLineHelp = "story-cli summary [ <remote-story> ]"

    override val oneLineHelp = "displays summary information about the story"

    override val multiLineHelp = List(
        "If a story name is provided, the remote server provides the information.",
        "If a story name is not provided, the local git repo provides the information.",
        "Summary information includes: premise, character names, and character roles.",
    )

    // TODO: snagged this from CloneCommand. Find a global spot for it
    val storyRe = """^[a-z][a-z0-9-/]*$""".r

    def parse(args: List[String]): Either[String, Option[os.SubPath]] = {
        @tailrec
        def loop(args: List[String], subPath: Option[os.SubPath]): Either[String, Option[os.SubPath]] = {
            if (args.isEmpty) Right(subPath)
            else {
                val arg = args.head
                arg match {
                    case storyRe(_*) => {
                        if (subPath.isDefined) Left("story argument has already been specified")
                        else loop(args.tail, Some(os.SubPath(arg)))
                    }
                    case unknown => Left(s"unknown argument: $unknown")
                }

            }
        }
        loop(args, None)
    }

    def render(title: String, ep: Either[String, Premise], ecs: Either[String, List[Character]]): List[String] = {
        s"title: $title" ::
        (ep match
            case Left(error) => s"ERROR: $error"
            case Right(p) => "premise: "+p.oneLine
        ) ::
        (ecs match
            case Left(error) => List(s"ERROR: $error")
            case Right(cs) => "characters:" :: cs.map{ c => s"  ${c.name} (${c.role})" }

        )
    }

    override def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error) => Left(error)
            case Right(ostory) => ostory match {
                // TODO: I don't appear to be using the local repo option
                case None => {
                    // sanity check we are in a local git repository
                    if (!os.exists(os.pwd / ".git" )) Left("current directory does not appear to be a local git repository")
                    else Right(render(os.pwd.last, Premise.read(os.pwd), Characters.read(os.pwd)))
                }
                case Some(subPath) => {
                    // remote dir summary
                    // on the server when a path has been specified, we assume local repo
                    val sf = StoriesFolder(go)
                    if (sf.isServer) {
                        val title = subPath.last
                        val dir = sf.serverStories / subPath / os.up / (title+".git")
                        Right(render(title, Premise.extract(dir), Characters.extract(dir)))
                    } else {
                        val cmd = ("story-cli" :: go.args).mkString(" ")
                        val result = os.proc("ssh", s"${go.userName}@${go.serverName}", cmd).call()
                        if (result.exitCode == 0) Right(result.out.lines().toList)
                        else Left(result.err.text()+result.out.text())
                    }
                }
            }
        }
    }
}
