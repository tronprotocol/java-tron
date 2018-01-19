The purpose for this folder is to provide developers with the infrastructure needed to run the application. These are 
not the only solutions for running the application, but just some that have been used to get the application running.

### What's in here?
Because the application requires several other components to be running, we've provided the developer an easy way to get
the dependencies up and running. Here are the dependencies:

#### Kafka
Data streaming service

#### Zookeeper
Stores shared data about consumers and brokers

### Docker
Located in `docker/docker-compose.yml`, simply run the following command to get kafka and zookeeper up and running:

```bash
cd docker
docker-compose up --build --force-recreate
```

*Alternatively, you can create a docker swarm with:*

```bash
cd docker
docker stack deploy -c docker-compose.yml tron-stack
```


### Kubernetes
TODO: provide this