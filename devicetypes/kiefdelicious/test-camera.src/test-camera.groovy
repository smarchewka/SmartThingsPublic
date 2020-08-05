/** 
 *  Generic IP Camera
 *
 *  Based on pstuart's Generic Video Camera and Generic Camera Device
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
*/

metadata {
	definition (name: "TEST Camera", namespace: "KiefDelicious", author: "tinlau") {
		capability "Image Capture"
		capability "Sensor"
		capability "Actuator"
		capability "Configuration"
		capability "Video Camera"
		capability "Video Capture"
		capability "Refresh"
		capability "Switch"
		capability "Motion Sensor"
        
        command "start"
        
		attribute "hubactionMode", "string"
	}

    preferences {
    input("CameraIP", "string", title:"Camera IP Address", description: "Please enter your camera's IP Address", required: false, displayDuringSetup: true)
    input("CameraUser", "string", title:"Camera User", description: "Please enter your camera's username", required: false, displayDuringSetup: true)
    input("CameraPassword", "string", title:"Camera Password", description: "Please enter your camera's password", required: false, displayDuringSetup: true)
	input("CameraRtspPort", "string", title:"Camera RTSP Port", description: "Please enter your camera's RTSP Port", defaultValue: 554 , required: false, displayDuringSetup: true)
	input("CameraPort", "string", title:"Camera Port", description: "Please enter your camera's Port", defaultValue: 80 , required: false, displayDuringSetup: true)
    input("CameraPath", "string", title:"Camera Path to Image", description: "Please enter the path to the image", defaultValue: "/Streaming/Channels/1/picture", required: false, displayDuringSetup: true)
    input("CameraPathVideo", "string", title:"Camera Path to Video", description: "Please enter the path to the video", defaultValue: "/Streaming/Channels/1", required: false, displayDuringSetup: true)
    input("CameraAuth", "bool", title:"Does Camera require User Auth?", description: "Please choose if the camera requires authentication (only basic is supported)", defaultValue: true, required: false, displayDuringSetup: true)
    input("CameraPostGet", "string", title:"Does Camera use a Post or Get, normally Get?", description: "Please choose if the camera uses a POST or a GET command to retreive the image", defaultValue: "GET", required: false, displayDuringSetup: true)
	}
    
	simulator {
    
	}

    tiles {
		multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 6, height: 4) {
			tileAttribute("device.switch", key: "CAMERA_STATUS") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", action: "switch.off", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", action: "switch.on", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", action: "refresh.refresh", backgroundColor: "#F22000")
			}

			tileAttribute("device.errorMessage", key: "CAMERA_ERROR_MESSAGE") {
				attributeState("errorMessage", label: "", value: "", defaultState: true)
			}

			tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", backgroundColor: "#F22000")
			}

			tileAttribute("device.startLive", key: "START_LIVE") {
				attributeState("live", action: "start", defaultState: true)
			}

			tileAttribute("device.stream", key: "STREAM_URL") {
				attributeState("activeURL", defaultState: true)
			}	
		}
		
    
        standardTile("take", "device.image", width: 2, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
            state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
        }

        standardTile("refresh", "device.alarmStatus", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "refresh", action:"polling.poll", icon:"st.secondary.refresh"
        }
        standardTile("blank", "device.image", width: 2, height: 1, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
            state "blank", label: "", action: "", icon: "", backgroundColor: "#FFFFFF"
        }
        standardTile("motion", "device.motionStatus", inactiveLabel: false, decoration: "flat") {
    		state "off", label: "off", action: "toggleMotion", icon: "st.motion.motion.inactive", backgroundColor: "#FFFFFF"
			state "on", label: "on", action: "toggleMotion", icon: "st.motion.motion.active",  backgroundColor: "#00ff00"
    	}
    	standardTile("refresh", "device.cameraStatus", inactiveLabel: false, decoration: "flat") {
        	state "refresh", action:"polling.poll", icon:"st.secondary.refresh"
    	}
        carouselTile("cameraDetails", "device.image", width: 6, height: 4) { }
        
        main "take"
        details(["videoPlayer", "take", "motion", "refresh", "cameraDetails"])
    }
}

//for photo
def parse(String description) {
    log.debug "Parsing '${description}'"
    def map = [:]
	def retResult = []
	def descMap = parseDescriptionAsMap(description)
	//Image
	def imageKey = descMap["tempImageKey"] ? descMap["tempImageKey"] : descMap["key"]
	if (imageKey) {
		storeTemporaryImage(imageKey, getPictureName())
	} 
}

// handle commands
def take() {
	def userpassascii = "${CameraUser}:${CameraPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def host = CameraIP 
    def hosthex = convertIPtoHex(host).toUpperCase() //thanks to @foxxyben for catching this
    def porthex = convertPortToHex(CameraPort).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex" 
    
    log.debug "The device id configured is: $device.deviceNetworkId"
    
    def path = CameraPath 
    log.debug "path is: $path"
    log.debug "Requires Auth: $CameraAuth"
    log.debug "Uses which method: $CameraPostGet"
    
    def headers = [:] 
    headers.put("HOST", "$host:$CameraPort")
   	if (CameraAuth) {
        headers.put("Authorization", userpass)
    }
    
    log.debug "The Header is $headers"
    
    def method = "GET"
    try {
    	if (CameraPostGet.toUpperCase() == "POST") {
        	method = "POST"
        	}
        }
    catch (Exception e) { // HACK to get around default values not setting in devices
    	settings.CameraPostGet = "GET"
        log.debug e
        log.debug "You must not of set the perference for the CameraPOSTGET option"
    }
    
    log.debug "The method is $method"
    
    try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: method,
    	path: path,
    	headers: headers
        )
        	
    hubAction.options = [outputMsgToS3:true]
    log.debug hubAction
    hubAction
    }
    catch (Exception e) {
    	log.debug "Hit Exception $e on $hubAction"
    }
    
}

def parseDescriptionAsMap(description) {
description.split(",").inject([:]) { map, param ->
def nameAndValue = param.split(":")
map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
}
}

private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
    log.debug pictureUuid
    def picName = device.deviceNetworkId.replaceAll(':', '') + "_$pictureUuid" + ".jpg"
	return picName
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
	log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
    log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}

//for video
private getVideoURL() {
    def videoURL = ""
    if (CameraAuth) {
    	videoURL = "rtsp://${CameraUser}:${CameraPassword}@${CameraIP}:${CameraRtspPort}${CameraPathVideo}"
    } else {
    	videoURL = "rtsp://${CameraIP}:${CameraRtspPort}${CameraPathVideo}"
    }
    return videoURL
}

mappings {
   path("/getInHomeURL") {
       action:
       [GET: "getInHomeURL"]
   }
}

def installed() {
	configure()
}

def updated() {
	configure()
}

// parse events into attributes
/*def parse(String description) {
	log.debug "Parsing '${description}'"
}*/

// handle commands
def configure() {
	log.debug "Executing 'configure'"
    sendEvent(name:"switch", value: "on")
}

def start() {
	log.trace "start()"
    def videoURL = getVideoURL()
    log.debug videoURL
	def dataLiveVideo = [
		OutHomeURL  : videoURL,
		InHomeURL   : videoURL,
		ThumbnailURL: "http://cdn.device-icons.smartthings.com/camera/dlink-indoor@2x.png",
		cookie      : [key: "key", value: "value"]
	]

	def event = [
		name           : "stream",
		value          : groovy.json.JsonOutput.toJson(dataLiveVideo).toString(),
		data		   : groovy.json.JsonOutput.toJson(dataLiveVideo),
		descriptionText: "Starting the livestream",
		eventType      : "VIDEO",
		displayed      : false,
		isStateChange  : true
	]
	sendEvent(event)
}

def getInHomeURL() {
	 [InHomeURL: getVideoURL()]
}
// for motion
def motion() {
	log.debug( "motion() state" );

def strUrl = "http://${CameraUser}:${CameraPassword}@${CameraIP}/IO/outputs/1/status{}";
	log.debug( "strUrl = ${strUrl}" );

	httpPost( strUrl ){

	}
}

def motionDisable = new XmlSlurper().parseText('''
	<IOPortStatus xmlns="http://www.hikvision.com/ver10/XMLSchema" version="1.0">
	<ioPortID>1</ioPortID>
	<ioPortType>output</ioPortType>
	<ioState>inactive</ioState>
	</IOPortStatus>
	   ''')
def motionEnable = new XmlSlurper().parseText('''
	<IOPortStatus xmlns="http://www.hikvision.com/ver10/XMLSchema" version="1.0">
	<ioPortID>1</ioPortID>
	<ioPortType>output</ioPortType>
	<ioState>active</ioState>
	</IOPortStatus>
	   ''')