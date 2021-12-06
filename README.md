# IctApiFinder: A tool for detecting incompatible API usages in Android Applications.
The repository hosts IctApifinder, a prototyping tool introduced in our paper "Understanding and Detecting Evolution-Induced Compatibility
Issues in Android Apps". 

# Artefact
The artefact of IctApiFinder is available at https://hub.docker.com/r/hdjay2013/ictapifinder.

The `data/SDK/`, `platforms/`, and `jdk1.8.0_201/` are only included in the Docker image due to the large size. 
So in order to build your own docker image or use the tool on your own machine, 
you need to first obtain them and put under the root directory, i.e., `IctApiFinder/`. 

You can use the following command to build your own docker image:
```
$ sudo docker build . -t imageName:tag
```

# Contributing to IctApiFinder
Contributions are always welcome. If you have a new feature or a bug fix that you would like to see in the official code repository, 
please open a merge request here on Github with a short description of what you have done.

# License
IctApiFinder is licenced under the GPL v3 license, see LICENSE file.
