echo "Installing needed resources..."
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt-get update
sudo apt-get install -f
sudo dpkg -i google-chrome-stable_current_amd64.deb
rm -f google-chrome-stable_current_amd64.deb
sudo apt-get install xvfb ruby openjdk-7-jdk
echo "Setting cronjob..."
echo "0 */4   * * *   cd $PWD && ruby crawlList.rb -e extensions/adblockplus-1.11.0.1580.crx -f $1 -t Regular4G" > crontabFile
crontab crontabFile
Xvfb :1 -screen 5 1024x768x8 &
