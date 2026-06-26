package org.kleemann.storycli.command

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}

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
                val sf = StoriesFolder(go)
                if (sf.isServer) {
                    // walk through all dirs until a directory that ends in .git
                    // we have to go one directory further since skipped files will not show up in the list
                    val customizedWalk = os.walk(
                        sf.serverStories,
                        skip = path => (path / os.up).last.endsWith(".git")
                    )
                    val ret = customizedWalk
                        .map{_.subRelativeTo(sf.serverStories).toString.stripSuffix(".git")}
                        .toList.sorted
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
