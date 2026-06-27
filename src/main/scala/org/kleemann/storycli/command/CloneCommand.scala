package org.kleemann.storycli.command

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}

object CloneCommand extends Command {

    val storyRe = """^[a-z][a-z0-9-/]*$""".r

    // returns single argument
    def parse(args: List[String]): Either[String, os.SubPath] = {
        if (args.length == 1) {
            val story = args.head
            if (storyRe.matches(story)) Right(os.SubPath(story))
            else Left("story argument must be letters, numbers, hyphens, and slashes")
        } else Left("usage: story-cli clone <dir/story>")
    }

    def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error) => Left(error)
            case Right(story) => {
                val sf = StoriesFolder(go)
                // verify that the story doesn't exist on the local machine
                val co = sf.checkouts / story
                if (os.exists(co)) Left(s"story is already checked out: ${co.toString}")
                else {
                    // see if the story exists on server
                    ListCommand.run(GlobalOptions(List(go.productionOption,"list"), go.isProduction, "list", Nil)) match {
                        case Left(error) => Left(error)
                        case Right(stories: List[String]) => {
                            if (!stories.contains(story.toString)) Left(s"specifed story does not exist on the server: $story")
                            else {
                                // create the parent checkout directory if necessary
                                os.makeDir.all(co / os.up)
                                // run a checkout command 
                                val repo =
                                    if (sf.isServer) "file://"          + ( sf.serverStories / story ) + ".git"
                                    else "ssh://" + go.serverName + ":" + ( sf.serverStories / story ) + ".git"
                                val result = os.proc("git", "clone", repo, co.toString).call()
                                if (result.exitCode == 0) Right(result.out.lines().toList)
                                else Left(result.err.text()+result.out.text())
                            }
                        }
                    }
                }
            }
        }
    }
}