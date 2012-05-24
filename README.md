grails-events-push
==================

Events-push is a client-side events bus based on the portable push library Atmosphere and Grails platform-core plugin for events
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
//will receive client events
@Listener saveTodo(Map data){
  //...
  event('savedTodo', data) // will trigger registered browsers
}
```

someView.gsp >
````gsp
<r:require module="grailsEvents"/>
<r:script>
 var grailsEvents = new grails.Events("http://localhost:8080/app/g-eventspush");
 grailsEvents.send('saveTodo', data); //will send data to server topic 'saveTodo'
 grailsEvents.on('savedTodo', function(data){...}); //will listen for server events on 'savedTodo' topic
</r:script>
``