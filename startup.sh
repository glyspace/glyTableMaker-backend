#!/bin/sh
# set environment variables
. ~/workspace/glyTableMaker-backend/bashrc
# start apache
cd ~/workspace/glyTableMaker-backend/apache && docker-compose up -d
# start postgres
cd ~/workspace/glyTableMaker-backend/postgres && docker-compose up -d
# start backend application
cd ~/workspace/glyTableMaker-backend && docker-compose up -d
# start frontend application
cd ~/workspace/glyTableMaker-frontend && docker-compose up -d --build && docker images -qf dangling=true | xargs docker rmi
