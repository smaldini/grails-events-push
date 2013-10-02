grails-events-push
==================

Events-push is a client-side events bus based on the superb portable push library [Atmosphere](https://github.com/Atmosphere/atmosphere)  and [Grails platform-core](https://github.com/grailsrocks/grails-platform-core) plugin for events
propagation/listening. It simply allows your client to listen to server-side events and push data. It uses WebSockets by default
and falls back to use Comet if required (server not compliant, browser too old...).
Events-push is a white-list broadcaster (client-side events scope is 'browser'). You will need to define which events can be
 propagated to the server by modifying the Events DSL to use 'browser' scope. To register listeners from client, you will need to
 define them as well. Ie:

MyEvents.groovy >

```groovy
events = {
    'savedTodo' browser:true // allows browser push on this topic
}
```


MyService.groovy >
```groovy
//will receive client events from 'saveTodo' topic
@Listener(namespace='browser') saveTodo(Map data){
  //...
  event('savedTodo', data) // will trigger registered browsers on 'savedTodo' topic
}
```

someView.gsp >
```gsp
<r:require module="grailsEvents"/>
<r:script>
 var grailsEvents = new grails.Events("http://localhost:8080/app/");
 grailsEvents.send('saveTodo', data); //will send data to server topic 'saveTodo'
 grailsEvents.on('savedTodo', function(data){...}); //will listen for server events on 'savedTodo' topic
</r:script>
```

You can find a full sample application using [Events-si](https://github.com/smaldini/grails-events-si), RabbitMQ, BackboneJS, coffeescript and CloudFoundry in
[Grails Todo repository](https://github.com/smaldini/grailsTodos).