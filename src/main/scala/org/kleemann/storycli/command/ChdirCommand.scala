package org.kleemann.storycli.command

import os.Path

import org.kleemann.storycli.{GlobalOptions}

object ChdirCommand extends Command {

    override val commandName = "cd"

    override val commandLineHelp = "story-cli cd <story-name-part>"

    override val oneLineHelp = "change the current working directory to the named story"

    override val multiLineHelp = List(
        "The story name includes all directory prefixes and separating slashes.",
        "The <story-name-part> can be a subset of the full story name.",
        "The command will fail if no stories are matched.",
        "The command will fail if multiple stories are matched."
    )

    case class Args(storyNamePart: String)

    def parse(in: List[String]): Either[String, Args] = {
        // TODO: I started matching against Nil, head :: tail, etc but I was getting sbt errors
        // and did this to try to fix it. It didn't work. Same with the following run() fn.
        in.length match {
            case 0 => Left("required argument <story-name-part> is missing")
            case 1 => Right(Args(in.head))
            case _ => Left("multiple arguments specified when only one is required")
        }
    }

    override def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error)                => Left(error)
            case Right(Args(storyNamePart)) => {
                ListCommand.localList(go, showPremise=false, showStatus=false) match {
                    case Left(error)    => Left(error)
                    case Right(stories) => {
                        val matches = stories.filter{ _.contains(storyNamePart) }
                        matches.length match {
                           case 0 => Left("no checked out stories match the specified criteria")
                           case 1 => Right(List(s"chdir ${matches.head}"))
                           case _ => Left("multiple stories match criteria:\n " + matches.mkString("\n") + "\n")                       }
                    }
                }
            }
        }
    }
}
