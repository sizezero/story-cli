package org.kleemann.storycli.command

import org.kleemann.storycli.{GlobalOptions, StoriesFolder, ServerStoriesFolder}

object ListCommand extends Command {

    // returns true if json is specified
    def parse(args: List[String]): Either[String, Boolean] = {
        if (args.length == 0) Right(false)
        else if (args.length == 1 && args.head == "--json") Right(true)
        else Left("usage: story-cli list [ --json ]")
    }

    def run(go: GlobalOptions): Either[String, List[String]] = {
        parse(go.rest) match {
            case Left(error) => Left(error)
            case Right(json) => {
                StoriesFolder.create(go) match {
                    case Right(sf: ServerStoriesFolder) => {
                        // walk through all dirs until a directory that ends in .git
                        // 3. Skip specific directories while walking (e.g., ignoring hidden or build folders)
                        val customizedWalk = os.walk(
                            sf.stories,
                            skip = path => (path / os.up).last.endsWith(".git")
                        )
                        val ret = customizedWalk
                            .map{_.subRelativeTo(sf.stories).toString.stripSuffix(".git")}
                            .toList.sorted
                        Right(ret)
                    }
                    case Left(sf: StoriesFolder) => {
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
}
