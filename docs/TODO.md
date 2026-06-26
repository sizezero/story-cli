
# TODO

* ~~make/use parse that takes commands and options~~
    * ~~production/development and server/client have to work immediately~~
    * ~~stub help command~~
* ~~make jar and shell wrapper; figure out easy deployment to production olivia and production hector~~
    * ~~install java on hector; test~~
        * olivia (ubuntu 24.04) is running openjdk 21
        * hector (arch) runs openjdk 26 ; tested
    * ~~build a jar that can be run on a common version of java~~
        * this is already built: build a jar that can be run on a common version of java
    * ~~wrapper script~~
        * use stock-notes as base
        * unlike stock-notes, this will not be run from the source tree, maybe it can be run from both?
    * ~~deploy script~~
        * shell script (and maybe symlink) are one time only
        * script takes olivia or hector argument
        * hector copies from olivia production location
* ~~add parsing tests~~
* ~~simple new command that just copies template to a file at the root of stories~~
    * server first
    * client second; figure out how to reuse code between client and server
* ~~list~~
    * start with remote named stories/repos
* checkout
    * simple non directory name
    * looks for existence in server
    * looks for collision in client
