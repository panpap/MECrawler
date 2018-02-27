if [ $# -eq 0 ]; then
    echo "No list for crontab is provided"
    exit 1
fi
echo "0 */4   * * *   cd $PWD && export DISPLAY=:1.5 && ruby crawlList.rb -e extensions/ -f $1 -r 3 -t Regular4G >> crawlOutput.out 2>&1" > crontabFile
crontab crontabFile
