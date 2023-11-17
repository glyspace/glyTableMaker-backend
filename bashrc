if [ -f ~/.secrets ]; then
    . ~/.secrets
fi

export MAVEN_DIR=$HOME/glytablemaker/maven
export POSTGRES_USER=glygen