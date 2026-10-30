# this script should be called from .bashrc with:
# source $HOME/bin/share/story-cli/src/scripts/set-alias.sh
story-cli() {
    STORY_CLI_OUT=$($HOME/bin/share/story-cli/story-cli "$@")
    if [ $? -eq 0 ]; then
        if [[ "$STORY_CLI_OUT" == "chdir "* ]]; then
            cd "$HOME/stories/checkouts/${STORY_CLI_OUT#chdir }"
        fi
        echo "$STORY_CLI_OUT"
        unset STORY_CLI_OUT
        return 0
    else
        echo "$STORY_CLI_OUT"
        unset STORY_CLI_OUT
        return 1
    fi
}