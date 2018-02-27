echo "Installing needed resources..."
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt-get update
sudo apt-get install -f
sudo dpkg -i google-chrome-stable_current_amd64.deb
rm -f google-chrome-stable_current_amd64.deb
sudo apt-get install xvfb ruby openjdk-7-jdk
Xvfb :1 -screen 5 1024x768x8 &
