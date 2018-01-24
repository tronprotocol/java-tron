FROM tronprotocol/centos7-jdk8
MAINTAINER tronprotocol

# Install dependencies && Download java-tron
RUN yum update -y && \
	yum install -y git && \
	git clone https://github.com/tronprotocol/java-tron.git /home/java-tron

# Change work directory
WORKDIR /home/java-tron

