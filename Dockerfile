FROM openjdk:17-alpine
VOLUME /tmp
ADD ./servicio-usuarios.jar servicio-usuarios.jar
ENTRYPOINT ["java","-jar","/servicio-usuarios.jar"]