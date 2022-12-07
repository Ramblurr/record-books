import {$, listen} from "./utils.js"
import {ActionMenu,Flyout} from "./widgets/action-menu.js"
//import encoderPath from './encoderWorker.min.js';

//// SETUP
htmx.onLoad(function(content) {
  htmx.config.defaultSettleDelay = 0;
  htmx.config.defaultSwapStyle = 'outerHTML';
})

document.body.addEventListener('htmx:beforeSwap', function(evt) {
    if(evt.detail.xhr.status === 404){
      evt.detail.shouldSwap = true;
    } else if(evt.detail.xhr.status === 400){
      evt.detail.shouldSwap = true;
    } else if(evt.detail.xhr.status === 422){
        // allow 422 responses to swap as we are using this as a signal that
        // a form was submitted with bad data and want to rerender with the
        // errors
        //
        // set isError to false to avoid error logging in console
        evt.detail.shouldSwap = true;
        evt.detail.isError = false;
    }
});


document.body.addEventListener("htmx:beforeRequest", function(evt) {
  NProgress.start();
});

document.body.addEventListener("htmx:beforeSwap", function(evt) {
  NProgress.inc(0.9);
});

document.body.addEventListener("htmx:afterSettle", function(evt) {
  NProgress.done();
  setTimeout(() => NProgress.remove(), 1000)
})

document.body.addEventListener("htmx:responseError", function(evt) {
  NProgress.done();
  NProgress.remove();
});

document.body.addEventListener("htmx:sendError", function(evt) {
  NProgress.done();
  NProgress.remove();
});

document.addEventListener('DOMContentLoaded', function() {
  listen('click', '[data-action-menu-trigger]', ActionMenu);
  listen('click', '[data-flyout-trigger]', Flyout);
});



const hideElement = element => {
  element.classList.add('hidden');
  element.setAttribute('aria-hidden', 'true');
}
const showElement = element => {
  element.classList.remove('hidden');
  element.setAttribute('aria-hidden', 'false');
}

const calcDuration = (buf) => {
  return new Promise((resolve, reject) => {
    var audioContext = new (window.AudioContext || window.webkitAudioContext)();

    audioContext.decodeAudioData(buf, function(buffer) {
      // Obtain the duration in seconds of the audio file (with milliseconds as well, a float value)
      var duration = buffer.duration;
      resolve(duration)
    });
  });
}


const decodeOgg = (typedArray) => {
  return new Promise((resolve, reject) => {
    var decoderWorker = new Worker("/js/decoderWorker.min.js");
    var wavWorker = new Worker("/js/waveWorker.min.js");
    var desiredSampleRate =8000;
    decoderWorker.postMessage({
      command:'init',
      decoderSampleRate: desiredSampleRate,
      outputBufferSampleRate: desiredSampleRate
    });

    wavWorker.postMessage({
      command:'init',
      wavBitDepth: 16,
      wavSampleRate: desiredSampleRate
    });

    decoderWorker.onmessage = function(e){

      // null means decoder is finished
      if (e.data === null) {
        wavWorker.postMessage({ command: 'done' });
      }

      // e.data contains decoded buffers as float32 values
      else {
        wavWorker.postMessage({
          command: 'encode',
          buffers: e.data
        }, e.data.map(function(typedArray){
          return typedArray.buffer;
        }));
      }
    };

    wavWorker.onmessage = function(e){
      if (e.data.message === "page") {
        console.log("Got wav", e.data)
        var fileName = new Date().toISOString() + ".wav";
        var dataBlob = new Blob( [ e.data.page ], { type: "audio/wav" } );
        var url = URL.createObjectURL( dataBlob );
        resolve({url: url, type: "audio/wav", typedArray, dataBlog, fileName})
      }
    };

    decoderWorker.postMessage({
      command: 'decode',
      pages: typedArray
    }, [typedArray.buffer] );
  });
};


  const getPermission = async () => {
    if(navigator.userAgent.match(/firefox|fxios/i)) {
      return true;
    } else {
      const result = await navigator.permissions.query({name: 'microphone'});
      if (result.state == 'granted') {
        return true;
      } else if (result.state == 'prompt') {
        return true;
      } else if (result.state == 'denied') {
        return false;
      }
      result.onchange = function () {};
    }
  }

const padLeadingZeros = (num, size) => {
  var s = num+"";
  while (s.length < size) s = "0" + s;
  return s;
}
const RecordingTimer = (el) => {
  return {
    el,
    seconds: 0,
    minutes: 0,
    hours: 0,
    start: function() {
      const self = this;
      this.timer = setInterval(function() {
        self.seconds++;
        if(self.seconds >= 60) {
          self.seconds = 0;
          self.minutes++;
        }
        if(self.minutes >= 60) {
          self.minutes = 0;
          self.hours++;
        }
        const hoursStr = self.hours > 0 ? `${hours}:` : "";
        el.innerText = `${hoursStr}${padLeadingZeros(self.minutes, 2)}:${padLeadingZeros(self.seconds, 2)}`;
      }, 1000);
    },
    resume: function() {
      this.start();
    },
    pause: function() {
      clearInterval(this.timer);
    },
    stop: function() {
      clearInterval(this.timer);
      self.seconds = 0;
      self.minutes = 0;
      self.hours = 0;
      el.innerText = "00:00";
    }
  };
}

const updateAudioPlayer = (result) => {
  const playbackContainer = $(".playback");
  const audio = $("audio", playbackContainer);
  audio.src = result.url;
  audio.type = result.type;
}

const hideAudioPlayer = () => {
  const playbackContainer = $(".playback");
  hideElement(playbackContainer);
}

const initAudioPlayer = async () => {

  let raf = null;
  const playbackContainer = $(".playback");
  const audio = $("audio", playbackContainer);
  const playButton = $(".play", playbackContainer)
  const pauseButton = $(".pause", playbackContainer)
  const durationContainer = $(".duration", playbackContainer)
  const seekSlider = $(".seek-slider", playbackContainer)
  const currentTimeContainer = $(".current-time", playbackContainer)
  const whilePlaying = () => {
    seekSlider.value = Math.floor(audio.currentTime);
    currentTimeContainer.textContent = calculateTime(seekSlider.value);
    audioPlayerContainer.style.setProperty('--seek-before-width', `${seekSlider.value / seekSlider.max * 100}%`);
    raf = requestAnimationFrame(whilePlaying);
  }
  const setSliderMax = (duration) => {
    seekSlider.max = Math.floor(duration);
  }
  const calculateTime = (secs) => {
    const minutes = Math.floor(secs / 60);
    const seconds = Math.floor(secs % 60);
    const returnedSeconds = seconds < 10 ? `0${seconds}` : `${seconds}`;
    return `${minutes}:${returnedSeconds}`;
  }
  const displayDuration = (duration) => {
    durationContainer.textContent = calculateTime(duration || audio.duration);
  }

  let playState = 'play';
  playButton.addEventListener('click', () => {
      audio.play();
      playState = 'pause';
      showElement(pauseButton);
      hideElement(playButton);
    });
  pauseButton.addEventListener('click', () => {
      audio.pause();
      playState = 'play';
      hideElement(pauseButton);
      showElement(playButton);
  });
  seekSlider.addEventListener('input', () => {
    currentTimeContainer.textContent = calculateTime(seekSlider.value);
  });
  seekSlider.addEventListener('change', () => {
    audio.currentTime = seekSlider.value;
  });
  seekSlider.addEventListener('input', () => {
    currentTimeContainer.textContent = calculateTime(seekSlider.value);
    if(!audio.paused) {
      cancelAnimationFrame(raf);
    }
  });

  seekSlider.addEventListener('change', () => {
    audio.currentTime = seekSlider.value;
    if(!audio.paused) {
      requestAnimationFrame(whilePlaying);
    }
  });
  audio.addEventListener('ended', () => {
    playState = 'ended';
    hideElement(pauseButton);
    showElement(playButton);
  });
  audio.addEventListener('loadedmetadata', (e) => {
    displayDuration(e.target.duration);
    setSliderMax(e.target.duration);
  });
  audio.addEventListener('timeupdate', () => {
    seekSlider.value = Math.floor(audio.currentTime);
  });
}


const initRecorder = async (startRecording)=> {
  const recorder = new Recorder({
    monitorGain: 0,
    recordingGain: 1,
    numberOfChannels: 1,
    encoderSampleRate: 48000,
    encoderPath: "/js/encoderWorker.min.js"
  });


  const recordIconContainer = $(".record-icon-container");
  const startButton =$(".start-recording", recordIconContainer)
  const stopButton = $(".stop-recording", recordIconContainer)
  const resumeButton = $(".resume-recording", recordIconContainer)
  const pauseButton = $(".pause-recording", recordIconContainer)
  const playbackContainer = $(".playback")
  pauseButton.addEventListener( "click", function(){ recorder.pause(); });
  resumeButton.addEventListener( "click", function(){ recorder.resume(); });
  stopButton.addEventListener( "click", function(){ recorder.stop(); });
  startButton.addEventListener( "click", function(){
    recorder.start().catch(function(e){
      console.error('Error encountered:', e.message );
    });
  });

  let recordingTimer = null;

  const setRecordButtonReset = () => {
    $(".record-label", startButton).textContent = "Start Over";
  }

  recorder.onstart = () => {
    console.log('Recorder is started');
    recordingTimer = RecordingTimer($(".recording-timer"));
    recordingTimer.start();
    hideAudioPlayer();
    hideElement(startButton);
    hideElement(resumeButton);
    showElement(stopButton);
    showElement(pauseButton);
  };

  recorder.onstop = () => {
    console.log('Recorder is stopped');
    recordingTimer.stop();
    showElement(startButton);
    hideElement(stopButton);
    hideElement(pauseButton);
    hideElement(resumeButton);
  };
  recorder.onpause = function(){
    console.log('Recorder is paused');
    recordingTimer.pause();
    hideElement(pauseButton);
    hideElement(startButton);
    showElement(resumeButton);
    showElement(stopButton);
  };

  recorder.onresume = function(){
    console.log('Recorder is resuming');
    recordingTimer.resume();
    hideElement(startButton);
    hideElement(resumeButton);
    showElement(stopButton);
    showElement(pauseButton);
  };
  recorder.onstreamerror = function(e){
    console.error('onstreamerror Error encountered: ' + e.message );
  };

  const wrapAudio = async (typedArray) => {
    const dataBlob = new Blob( [typedArray], { type: 'audio/ogg' } );
    const fileName = new Date().toISOString() + ".opus";
    const url = URL.createObjectURL( dataBlob );
    //const duration = await calcDuration(dataBlob);
    console.log("got audio", fileName)
    console.log(url)
    return {
      typedArray,
      url,
      fileName,
      dataBlob,
     // duration,
      type: "audio/ogg"
    }
  }
  recorder.ondataavailable = async ( typedArray ) => {
    console.log('Data received');
    const result = await wrapAudio(typedArray);
    //await decodeOgg(result.typedArray);
    //updateAudioPlayer({url: url, type: "audio/wav"})
    setRecordButtonReset();
    showElement(playbackContainer);
    updateAudioPlayer(result);
  };

  if(startRecording) {
    recorder.start();
  }
}

document.addEventListener('DOMContentLoaded', function() {
  if(!Recorder.isRecordingSupported())
    console.error("RECORDING IS NOT SUPPORTED")

  const recordIconContainer = $(".record-icon-container");
  const startButton =$(".start-recording", recordIconContainer)
  startButton.addEventListener("click", async function(){
    if(await getPermission())
      initRecorder(true);
  });

  initAudioPlayer();
});


document.addEventListener("adobe_dc_view_sdk.ready", function(){
  var adobeDCView = new AdobeDC.View({clientId: "0a9e87b45dd44e9fb6cf1379494bf465", divId: "adobe-dc-view"});
  adobeDCView.previewFile({
    content:{location: {url: "https://documentservices.adobe.com/view-sdk-demo/PDFs/Bodea Brochure.pdf"}},
    metaData:{fileName: "Bodea Brochure.pdf"}
  }, {embedMode: "IN_LINE"});
});
