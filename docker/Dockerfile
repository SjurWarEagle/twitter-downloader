FROM maven:3.8.6-openjdk-18

RUN mkdir -p /project
COPY . /project
WORKDIR /project

RUN mvn package

CMD ["/bin/bash", "-c", "/project/start_all.sh"]
#TODO make smaller runtime image!
