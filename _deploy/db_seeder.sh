#!/usr/bin/env bash
# Vars
TARGET_DB_USER=xwiki
TARGET_DB_HOST=18.235.115.102
TARGET_DB_PORT=5000

DB_DUMP_FILENAME="db_seed.out"
CURRENT_DIR=$(pwd)
DUMP_LOCATION=$CURRENT_DIR"/data/"$DB_DUMP_FILENAME

while getopts u:d:p:h: option
do
  case "${option}" in
    h) TARGET_DB_HOST=${OPTARG};;
    p) TARGET_DB_PORT=${OPTARG};;
    u) TARGET_DB_USER=${OPTARG};;
    p) FORMAT=${OPTARG};;
  esac
done

PG_RESTORE_CMD="pg_restore --verbose --create -h $TARGET_DB_HOST -p $TARGET_DB_PORT -U $TARGET_DB_USER -d xwiki \"$DUMP_LOCATION\""

eval("$PG_RESTORE_CMD")
