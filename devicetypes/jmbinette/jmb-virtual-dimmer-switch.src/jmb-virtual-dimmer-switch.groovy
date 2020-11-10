/**
 *  Copyright 2017 SmartThings
 *
 *  Provides a virtual dimmer switch
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
    definition (name: "JMB - Virtual Dimmer Switch", namespace: "jmbinette", author: "JM Binette") {
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Switch Level"
        
        attribute "brightness", "number"
        attribute "state", "string"
        
        command "jsoncommand"
        command "processMQTT"
    }

    preferences {}

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#FFFFFF", nextState:"turningOn", defaultState: true
                attributeState "turningOn", label:'Turning On', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#00A0DC", nextState:"turningOn"
                attributeState "turningOff", label:'Turning Off', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#FFFFFF", nextState:"turningOff"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action: "setLevel"
            }
            tileAttribute ("brightnessLabel", key: "SECONDARY_CONTROL") {
                attributeState "Brightness", label: '${name}', defaultState: true
            }
        }

        standardTile("explicitOn", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "On", action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff"
        }
        standardTile("explicitOff", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "Off", action: "switch.off", icon: "st.Home.home30", backgroundColor: "#ffffff"
        }
        controlTile("levelSlider", "device.level", "slider", width: 2, height: 2, inactiveLabel: false, range: "(1..100)") {
            state "physicalLevel", action: "switch level.setLevel"
        }

        main(["switch"])
        details(["switch", "explicitOn", "explicitOff", "levelSlider"])

    }
}

def parse(String description) {
}

def on() {
    log.trace "Executing 'on'"
    turnOn()
}

def off() {
    log.trace "Executing 'off'"
    turnOff()
}

def onMQTT() {
	//Execution lorsque l'action provient de MQTT
    log.trace "Executing 'on' from a MQTT request"
    turnOnMQTT()
}

def offMQTT() {
	//Execution lorsque l'action provient de MQTT
    log.trace "Executing 'off' from a MQTT request"
    turnOffMQTT()
}


def setLevelMQTT(value) {
	//Execution lorsque le niveau provient de MQTT
    //Integer scaledvalue = value*2.55;
    log.trace "Executing setLevel from MQTT, ST: $value, ESP: $value"
    Map levelEventMap = buildSetLevelEvent(value)
    if (levelEventMap.value == 0) {
        turnOff()
        // notice that we don't set the level to 0'
    } else {
        //implicitOn()
        sendEvent(levelEventMap)
        //sendEvent(name: "jsoncommand", value: "{\"state\":\"ON\",\"brightness\":${value}}")
        //sendEvent(name: "jsoncommand", value: "{\"brightness\":${scaledvalue}}")

    }
}

def setLevel(value) {
	//Execution lorsque le niveau provient de ST, on notifie MQTT
    Integer scaledvalue = value*2.55;
    log.trace "Executing setLevel from ST, ST: $value, ESP: $scaledvalue"
    Map levelEventMap = buildSetLevelEvent(value)
    if (levelEventMap.value == 0) {
        //turnOff()
        // notice that we don't set the level to 0'
    } else {
        //implicitOn()
        sendEvent(levelEventMap)
        //sendEvent(name: "jsoncommand", value: "{\"state\":\"ON\",\"brightness\":${value}}")
        sendEvent(name: "jsoncommand", value: "{\"brightness\":${scaledvalue}}")

    }
}

def processMQTT(attribute, value){
    //sendEvent(name: attribute, value: value)
	switch (attribute) {
		case 'update':
			updateTiles(value);
			break;
        case 'brightness':
        	Float scaledvalue = (value/2.55);
    		//Integer IntScaledValue = scaledvalue.toInteger();
        	//Double scaledvalue = Double.parseDouble(value)/2.55;
            log.debug "Processing ${attribute} Event:  ${value} from MQTT for device: ${device} (Scaled Value: $scaledvalue)"
        	setLevelMQTT(scaledvalue as int)
            break;
        case 'state':    
            log.debug "Processing ${attribute} Event:  ${value} from MQTT for device: ${device}"
            if (value == "OFF") {
                turnOffMQTT()
            } else if (value == "ON") {
                turnOnMQTT()
            }
            break;  
		default:
			break;
	}
}

private Map buildSetLevelEvent(value) {
    def intValue = value as Integer
    def newLevel = Math.max(Math.min(intValue, 100), 0)
    Map eventMap = [name: "level", value: newLevel, unit: "", isStateChange: true]
    return eventMap
}


//private implicitOn() {
//    if (device.currentValue("switch") != "on") {
//        turnOn()
//    }
//}
//private implicitOff() {
//    if (device.currentValue("switch") != "off") {
//        turnOff()
//    }
//}

private turnOnMQTT() {
    sendEvent(name: "switch", value: "on")
    
}

private turnOffMQTT() {
    sendEvent(name: "switch", value: "off")
}



private turnOn() {
	//Execution lorsque l'action provient de ST, on notifie MQTT
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "jsoncommand", value: "{\"state\":\"ON\"}")

    
}

private turnOff() {
	//Execution lorsque l'action provient de ST, on notifie MQTT
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "jsoncommand", value: "{\"state\":\"OFF\"}")

}



def installed() {
    setLevel(100)
}
