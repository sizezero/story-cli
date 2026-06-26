package org.kleemann.storycli.command

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}

object NewCommand extends Command {

    /**
      * This performs file system commands and so is not functional.
      *
      * @param go
      * @param rest
      * @return
      */
    def run(go: GlobalOptions): Either[String, List[String]] = {

        val storyDirRe = """^[a-z][a-z0-9-]*$""".r
        if (go.rest.length != 1 || !storyDirRe.matches(go.rest.head))
            Left(s"new command argument but be single story name of letters, numbers, and hyphen: ${go.rest.head}")
        else {
            val arg = go.rest.head
            val sf = StoriesFolder(go)
            if (sf.isServer) {
                if (!os.exists(sf.serverTemplate)) Left(s"source directory doesn't exist: ${sf.serverTemplate}")
                else {
                    val dst = sf.serverStories / (arg+".git")
                    if (os.exists(dst)) Left(s"target story repo already exists: ${dst.toString}")
                    else {
                        // os.copy() does not preserve permissions
                        val result = os.proc("cp", "-a", sf.serverTemplate, dst).call()
                        if (result.exitCode == 0) Right(List(s"Created story repo: ${arg}"))
                        else Left(result.err.text()+result.out.text())
                    }
                }
            } else {
                // client is always in the production location so just pass the same --development or --production flag to the server
                val cmd = ("story-cli" :: go.args).mkString(" ")
                val result = os.proc("ssh", s"${go.userName}@${go.serverName}", cmd).call()
                if (result.exitCode == 0) Right(result.out.lines().toList)
                else Left(result.err.text()+result.out.text())
            }
        }
    }

}
