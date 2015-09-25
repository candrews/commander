"use strict";
$(document).ready(function() {
    var currentDictationRequestId = null;
    
    // defined a connection to a new socket endpoint
    var socket = new SockJS('/stomp');

    var stompClient = Stomp.over(socket);

    stompClient.connect({ }, function(frame) {
        
        stompClient.subscribe("/user/topic/console", function(data) {
        	var consoleData = JSON.parse(data.body);
        	log(consoleData.text);
        });
        
        stompClient.subscribe("/user/topic/shutdown", function(data) {
        	var shutdownData = JSON.parse(data.body);
        	log(shutdownData.message);
        });
        
        stompClient.subscribe("/user/topic/request", function(data) {
            var requestToClient = JSON.parse(data.body);
            var requestClass = requestToClient['@class'];
            switch(requestClass){
            case "com.integralblue.commander.web.message.SpeakerRequestToClient":
                play(requestToClient.mimeType, requestToClient.audio, function(){
	            	stompClient.send("/app/response",{},JSON.stringify({
	            		'@class': 'com.integralblue.commander.web.message.SpeakerResponseFromClient',
	            		'id': requestToClient.id
	            	}));
                });
                break;
            case "com.integralblue.commander.web.message.SynthesisRequestToClient":
            	var onend = function(){
	            	stompClient.send("/app/response",{},JSON.stringify({
	            		'@class': 'com.integralblue.commander.web.message.SynthesisResponseFromClient',
	            		'id': requestToClient.id
	            	}));
            	};
            	if ('speechSynthesis' in window) {
	            	var utterance = new SpeechSynthesisUtterance(requestToClient.text);
	            	utterance.onend = onend;
	            	window.speechSynthesis.speak(utterance);
            	}else{
            		log("Synthesis not supported: " + requestToClient.text);
            		onend();
            	}
            	break;
            case "com.integralblue.commander.web.message.DictationRequestToClient":
            	currentDictationRequestId = requestToClient.id;
            	$('#textInput').prop('disabled', false).focus();
            	break;
            default:
            	throw new Error('Unknown request @class: ' + requestClass);
            }
        });
        
        stompClient.send("/app/start");
        
        $(".connecting").hide();
    }, function(message) {
        alert("An error occurred: " + message);
    });
    
    $('#textInput').on("keyup", function (event) {
    	if (event.keyCode==13) {
    		if(currentDictationRequestId==null){
    			throw new Error("currentDictationRequestId is null, this should never happen");
    		}
        	stompClient.send("/app/response",{},JSON.stringify({
        		'@class': 'com.integralblue.commander.web.message.DictationResponseFromClient',
        		'id': currentDictationRequestId,
        		'text': $('#textInput').val()
        	}));
        	$('#textInput').val("");
        	currentDictationRequestId = null;
        	$('#textInput').prop('disabled', true);
   		}
    });
    
    function log(text){
    	$(".console .messages").append($("<ol />").text(text));
    }

    
    // play base64 encoded sound
    var df = document.createDocumentFragment();
    function play(mimeType, base64audio, callback){
    	// Prefer AudioContext over the HTML5 audio element because mobile browsers on Android and iOS won't play HTML5 audio element context unless the playing of it is triggered by a user interaction
    	// Fix up for prefixing
		window.AudioContext = window.AudioContext||window.webkitAudioContext;
    	if(window.AudioContext){
	    	var audioContext = new AudioContext();
	    	var source = audioContext.createBufferSource();
	    	audioContext.decodeAudioData(base64ToArrayBuffer(base64audio), function(buffer) {
	    	   source.buffer = buffer;
	    	   source.connect(audioContext.destination);
	    	   source.start(0);
	    	   setTimeout(callback, buffer.duration*1000);
	    	});
    	}else{
	        var snd = new Audio("data:" + mimeType + ";base64," + base64audio);
	        df.appendChild(snd); // keep in fragment until finished playing
	        snd.addEventListener('ended', function () {df.removeChild(snd);callback();});
	        snd.play();
    	}
    }
    
    function base64ToArrayBuffer(base64) {
	  var binaryString =  window.atob(base64);
	  var len = binaryString.length;
	  var bytes = new Uint8Array( len );
	  for (var i = 0; i < len; i++)        {
	    bytes[i] = binaryString.charCodeAt(i);
	  }
	  return bytes.buffer;
	}
});
