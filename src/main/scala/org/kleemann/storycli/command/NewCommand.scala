package org.kleemann.storycli.command

import org.kleemann.storycli.{GlobalOptions, StoriesFolder, ServerStoriesFolder}

object NewCommand extends Command {

    /**
      * This performs file system commands and so is not functional.
      *
      * @param go
      * @param rest
      * @return
      */
    def run(go: GlobalOptions, rest: List[String]): Either[String, List[String]] = {

        val storyDirRe = """^[a-z][a-z0-9-]*$""".r
        if (rest.length != 1 || !storyDirRe.matches(rest.head))
            Left(s"new command argument but be single story name of letters, numbers, and hyphen: ${rest.head}")
        else {
            val arg = rest.head
            StoriesFolder.create(go) match {
                case Left(sf: StoriesFolder) => {
                    // client is always in the production location so just pass isProduction as a development flag
                    // TODO: it could be helpful to have the full command line available in go; I can't see why we wouldn't pass it here
                    val flag = if (go.isProduction) "--production" else "--development"
                    val cmd = s"story-cli ${flag} new ${arg}"
                    val result = os.proc("ssh", s"${go.userName}@${go.serverName}", cmd).call()
                    if (result.exitCode == 0) Right(result.out.lines().toList)
                    else Left(result.err.text()+result.out.text())
                }
                case Right(sf: ServerStoriesFolder) => {
                    if (!os.exists(sf.template)) Left(s"source directory doesn't exist: ${sf.template}")
                    else {
                        val dst = sf.stories / (arg+".git")
                        if (os.exists(dst)) Left(s"target story repo already exists: ${dst.toString}")
                        else {
                            // os.copy() does not preserve permissions
                            val result = os.proc("cp", "-a", sf.template, dst).call()
                            if (result.exitCode == 0) Right(List(s"Created story repo: ${arg}"))
                            else Left(result.err.text()+result.out.text())
                        }
                    }
                }
            }
        }
    }

}
