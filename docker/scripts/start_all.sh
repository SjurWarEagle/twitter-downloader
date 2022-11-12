#nginx -g daemon off
cd /project
#TODO do not use src folder build build, that requires less space and tools
java -jar target/twitterImageDownloader-1.0-SNAPSHOT.jar
