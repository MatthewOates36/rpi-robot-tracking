sudo apt-get install ffmpeg libavcodec-dev libavformat-dev
sudo apt-get install ffmpeg libavcodec-extra-53
sudo apt-get install libav-tools

sudo apt install libjasper1

scp out/artifacts/rpi_robot_tracking_jar/rpi-robot-tracking.jar pi@vision-rpi.local:~

export LD_LIBRARY_PATH="/home/pi/libs:$LD_LIBRARY_PATH"
java -Djava.library.path="libs/" -jar rpi-robot-tracking.jar
