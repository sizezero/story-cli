
# Design

`story-cli` provides several independent functions that allow the user to provision stories represented as .md files accross a set of private git repos. The functions allow the user to create a new story repo from a template, list all remote stories as well as selective metadata, checkout remote stories, and generate summary story structure information based on embedded metadata in the story .md file.

Since these functions are distict, they are discussed in separate sections

Some design principles:
* markdown (.md) is the primary format for all story files and support documents
* The development model is terminal based and includes tools such as emacs, tmux, w3m, sc-im, and pandoc.
* Each story is it's own git repository so that branches and tags work for each individual story. We could have used a single repo for all stories but that would result in many confusingly named branches and tags. Stories are distinct from each other and decoupled so it doesn't make sense to have each one in the same repo.
* The same app can be run on a server and a client. Some commands can only be run on the server.

## Directory Structure

Much of the application is just about the directory structure. The user could just run manual git commands and file copies that follow this structure and not even use this app. The app is just a convenience.

```
~/stories/
    repos/
        template.git/
        stories/
    development/
        story-cli/
        repos/
    checkouts/
```

**~/stories/** : the absolute location for all data files. This includes git repositories on the server, git checkouts on the client, and app development.

**~/stories/repos/** : this directory contains the bare git repos. This directory exists only on the server. The absence of this file is what tells the application that it is a client and not a server.

**~/stories/repos/template.git/** : This is the template from which all new stories are based. It contains documents for premise, characters, setting, and the first draft. It can improve and change over time. When a new story is desired it should be created via `cp -a template.git repos/somedir/someotherdir/newstoryname.git`

**~/stories/repos/stories/** : this is the directory that contains the bare git repositories that contain the actual stories

**developlment/story-cli/** : This is an optional checked out copy of this repository. There is no programmatic reason that it needs to be in this directory, it is just a convention.

**development/repos/** : This follows the same structure as `~/stories/repos/stories/` It is used by the app when run in debug mode.

**~/stories/checkouts/** : This is the default location for story checkouts. It follows the same structure as `~/stories/repos/stories/` except bare git repos named *mystory*.git/ become checked out git repos named *mystory*/

## Production and Debug mode

The application runs in development as a set of java (scala) classes. When deployed it is a shell script that calls `java -jar story-cli.jar`. When it runs on the client, it sometimes uses ssh to reach the server and run the shell script wrapper.

In all these cases the problem either has to run in production mode where it uses production data in `~/stories/repos/stories/` or development mode where it uses development data in `~/stories/development/repos/`.

Here are some simple rules for how the app determines whether or not it is in production or development mode.

TODO: figure out how the location tester works when run from .class files vs. run from a jar.

* If the application is running from .class files and not a jar then it only runs in development mode. The global option `--production` produces an error.
* If the application is running as a jar that is located beneath the `~/bin/` directory then it can run in either development or production mode. It defaults to production mode.
* If the application is running as a jar that is *not* located beneath the `~/bin/` directory then it will only run in development mode. The global option `--production` produces an error.
* If the application is running as a client and calls the server, then it always calls the server in the same mode that it is running in. TODO: not sure about this.

## Command Line

All this isn't an explict function per se, it ties all the functionality together and thus must share a coherent design.

The command line has the structure:

```
story-cli [ global options ] <command> [ command specific options and arguments ]
```

Global options:

**-h, --help** : display help text

**--production** : tells the application to run in production mode. The main difference is that it will use the `~/stories/repos/` directory. If the application is not located under `~/bin/` an error is produced.

**--development** : tells the application to run in development mode. The main difference is that it will use the `~/stories/development/repos/` directory.

**\<command>** : one of several commands. These are documented later.

## Commands

### new

Copy template story to a story with a new name.

`new [ --newpath ] <name>`

The name may be prefixed with a partial directory path.

### list

list remote stories? list local stories? I think listing all remote stories is important. Maybe there can be a flag to list either local or remote. Separate flags have the detail level from name only to lots of metadata. Make name filter optional. It's so generic maybe it should be called view or summary.

### checkout

checkout a single story? checkout all stories? What about refresh? I probably don't care about refresh.

### outline

creates a spreadsheet of incidents. Output can be .csv or .sc.