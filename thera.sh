HELP="Usage: thera <command>, where <command> is one of:
  start - Start Thera
  stop  - Stop Thera
  bash  - open bash console at Thera's Docker image"
SELF_DIR=`pwd`

function start_thera {
  docker run -td \
    -v $SELF_DIR/volumes/home:/root \
    -v $SELF_DIR:/root/thera \
    -v /Users/anatolii/.ivy2:/root/.ivy2 \
    -p 8888:8888 \
    --name thera \
    --rm \
    thera:latest
}

function stop_thera {
  docker stop thera
}

function run_on_thera {
  docker exec -ti thera $@
}

case $1 in
    start) start_thera;;
     stop) stop_thera;;
  restart) stop_thera && start_thera;;

  build) run_on_thera amm build.sc;;
   bash) run_on_thera bash;;

    '') echo -e "$HELP";;
     *) echo -e "Unknown command: $1\n$HELP";;
esac
