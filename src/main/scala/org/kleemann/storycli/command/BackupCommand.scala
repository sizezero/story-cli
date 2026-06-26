package org.kleemann.storycli.command

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.kleemann.storycli.{GlobalOptions, StoriesFolder}

object BackupCommand extends Command {

    def run(go: GlobalOptions): Either[String, List[String]] = {
        val sf = StoriesFolder(go)
        if (go.rest.length != 0) Left("backup command takes no arguments")
        else if (!sf.isServer) Left("the backup command can only be run on the server")
        else {
            val src = sf.top
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            val formattedDateTime = now.format(formatter)
            val dst: os.Path = os.home / "to-delete" / "story-cli" / formattedDateTime
            os.makeDir.all(dst / os.up)
            // os.copy() does not preserve permissions so we need "cp -a"
            val result = os.proc("cp", "-a", src, dst).call()
            if (result.exitCode == 0) Right(List(s"Created backup: ${dst}"))
            else Left(result.err.text()+result.out.text())
        }
    }
}