grails-events-push
==================

Events-push is a client-side events bus based on the superbe portable push library [Atmosphere](https://github.com/Atmosphere/atmosphere)  and [Grails platform-core](https://github.com/grailsrocks/grails-platform-core) plugin for events
propagation/listening. It simply allows your client to listen to server-side events and push data. It uses WebSockets by default
and failbacks to Comet method if required (server not compliant, browser too old...).
Events-push is a white-list broadcaster (client-side events scope is 'browser'). You will need to define which events can be
 propagated to server by using Events DSL to use 'browser' scope. To register listeners from client, you will need to
 define them too. Ie:

MyEvents.groovy >

```groovy
events = {
    'saveTodo' scope:'*' // allows both server (scope:'app' or 'pluginName') and client (scope:'browser') to send data over this topic
    'savedTodo' browser:true // allows browser push on this topic
}
```


MyService.groovy >
```groovy
//will receive client events from 'saveTodo' topic
@Listener saveTodo(Map data){
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

You can find a full sample using [Events-si](https://github.com/smaldini/grails-events-si), RabbitMQ, BackboneJS, coffeescript and CloudFoundry in
[Grails Todo repositry](https://github.com/smaldini/grailsTodos).