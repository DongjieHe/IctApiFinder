FROM ubuntu:18.04
ENV workdir /ictapifinder
WORKDIR $workdir
ENV user root
USER $user
COPY benchmarks/ $workdir/benchmarks
COPY platforms/ $workdir/platforms/
COPY data/ $workdir/data/
COPY gradle/ $workdir/gradle/
COPY src $workdir/src/
COPY libs $workdir/libs/
COPY build.gradle $workdir/
COPY settings.gradle $workdir/
# COPY finder.sh $workdir
COPY gradlew $workdir/
COPY doc/paper.pdf $workdir/
COPY doc/Artifact-Manual.pdf $workdir/
COPY LICENSE $workdir/
COPY jdk1.8.0_201/ /usr/local/java/jdk8
RUN apt update
RUN apt install -y vim
RUN apt install -y libtcmalloc-minimal4
RUN apt install -y libgoogle-perftools4
RUN apt install -y protobuf-compiler
RUN apt install -y libprotobuf-dev
RUN apt install -y libprotobuf-java
RUN apt install -y libboost-date-time1.65.1
RUN apt install -y libboost-filesystem1.65.1
RUN apt install -y libboost-iostreams1.65.1
RUN apt install -y libboost-program-options1.65.1
RUN apt install -y libboost-date-time1.65.1
RUN apt install -y libboost-system1.65.1
RUN apt install -y libboost-thread1.65.1
RUN apt install -y libicu60
RUN apt install -y libboost-regex1.65.1
RUN apt install -y libcppunit-1.14-0
RUN apt install -y locales
RUN locale-gen en_US.UTF-8
RUN dpkg -i $workdir/libs/pa-datalog_0.5-1bionic.deb
RUN apt install -f
RUN mv $workdir/data/bashrc $workdir/.bashrc
ENV JAVA_HOME /usr/local/java/jdk8/
ENV LC_ALL en_US.UTF-8
ENV LB_PAGER_FORCE_START 1
ENV LOGICBLOX_HOME /opt/lb/pa-datalog
ENV PATH ${LOGICBLOX_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV LD_LIBRARY_PATH /opt/lb/pa-datalog/lib/cpp:
CMD /bin/bash
