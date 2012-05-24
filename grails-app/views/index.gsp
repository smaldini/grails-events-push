<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
  <r:require modules="grailsEvents"/>
  <meta name='layout' content='main'/>
  <r:script>
    $(document).ready(function () {

      var grailsEvents = new grails.Events(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + "/events-push");

      function getKeyCode(ev) {
        if (window.event) return window.event.keyCode;
        return ev.keyCode;
      }

      function subscribe() {
        grailsEvents.on($('#topic').val(), function (data) {
          $("#messages").append("<div>" + data.message + "</div>")
        });
        grailsEvents.on("afterInsert", function (data) {
          $("#messages").append("<div>" + $.stringifyJSON(data) + "</div>");
        });
      }

      function connect() {
        $('#phrase').val('');
        $('#sendMessage').attr('class','');
        $('#phrase').focus();
        subscribe();
        $('#connect').val("Switch transport");
      }

      $('#connect').click(function (event) {
        if ($('#topic').val() == '') {
          alert("Please enter a PubSub topic to subscribe");
          return;
        }
        connect();
      });

      $('#topic').keyup(function (event) {
        $('#sendMessage').attr('class','hidden');
        var keyc = getKeyCode(event);
        if (keyc == 13 || keyc == 10) {
          connect();
          return false;
        }
      });

      $('#phrase').attr('autocomplete', 'OFF');
      $('#phrase').keyup(function (event) {
        var keyc = getKeyCode(event);
        if (keyc == 13 || keyc == 10) {
          grailsEvents.send($('#topic').val(), {message:$('#phrase').val()});
          $('#phrase').val('');
          return false;
        }
        return true;
      });

      $('#send_message').click(function (event) {
        if ($('#topic').val() == '') {
          alert("Please enter a message to publish");
          return;
        }
        grailsEvents.send($('#topic').val(), {message:$('#phrase').val()});
        $('#phrase').val('');
        return false;
      });


      $('#topic').focus();
    });
  </r:script>
</head>

<body>
<h2>Select topic to subscribe</h2>

<div id='pubsub'>
  <input id='topic' type='text' value="sampleBro"/>
</div>

  <input id='connect' class='button' type='submit' name='connect' value='Connect'/>
<br/>

<h2 id="s_h" class='hidden'>Publish Topic</h2>

<div id='sendMessage' class='hidden'>
  <input id='phrase' type='text'/>
  <input id='send_message' class='button' type='submit' name='Publish' value='Publish Message'/>
</div>
<br/>

<h2>Real Time PubSub Update</h2>
<ul id='messages'></ul>
</body>
</html>