FROM maven:3.8.6-openjdk-18

RUN mkdir -p /project
RUN mkdir -p /project/images

COPY . /project
COPY docker/scripts/start_all.sh /project
RUN chmod 777 docker/scripts/start_all.sh
WORKDIR /project

RUN mvn clean package

CMD ["/bin/bash", "-c", "/project/start_all.sh"]
#TODO make smaller runtime image!
