if [ $# -eq 0 ]; then
    echo "No list for crontab is provided"
    exit 1
fi
echo "Installing needed resources..."
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt-get update
sudo apt-get install -f
sudo dpkg -i google-chrome-stable_current_amd64.deb
rm -f google-chrome-stable_current_amd64.deb
sudo apt-get install xvfb ruby openjdk-7-jdk
echo "Setting cronjob..."
echo "0 */4   * * *   cd $PWD && export DISPLAY=:1.5 && ruby crawlList.rb -e extensions/ -f $1 -r 3 -t Regular4G >> crawlOutput.out 2>&1" > crontabFile
crontab crontabFile
Xvfb :1 -screen 5 1024x768x8 &
