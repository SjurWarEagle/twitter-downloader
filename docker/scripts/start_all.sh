#nginx -g daemon off
service nginx restart
cd /project/server/dist/server/src
#TODO do not use src folder build build, that requires less space and tools
node main
