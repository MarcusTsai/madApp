(function() {
    'use strict';
    madAPP.controller('logicBuilderController', logicBuilderController);

    function logicBuilderController(sensorService, $window) {
        var lb = this;
        var dbRef = firebase.database().ref();
        var appRef = dbRef.child('apps');

        lb.init = function() {
            lb.sensor_list = sensorService.getSensors();
            lb.actuator_list = sensorService.getActuators();
            lb.new_sensor = lb.sensor_list[0];
            lb.new_actuator = lb.actuator_list[0];
            lb.added_actuators = [];
            lb.added_sensors = [];
        };
        lb.init();

        lb.addSensor = function() {
            lb.new_sensor.disable = true;
            lb.added_sensors.push(lb.new_sensor);
        };

        lb.addActuator = function() {
            lb.new_actuator.disable = true;
            lb.added_actuators.push(lb.new_actuator);
        };

        lb.removeSensor = function(idx) {
            lb.added_sensors[idx].disable = false;
            lb.added_sensors.splice(idx, 1);
        };

        lb.removeActuator = function(idx) {
            lb.added_actuators[idx].disable = false;
            lb.added_actuators.splice(idx, 1);
        };

        lb.createdApp = function() {
            if (lb.added_sensors.length === 0 || lb.added_actuators.length === 0) {
                alert("Please add at least one sensor and one actuator");
                return;
            }
            lb.new_app.created_at = new Date().toLocaleString();
            lb.new_app.default_config = {};
            for (var i = lb.added_sensors.length - 1; i >= 0; i--) {
                var sensor = lb.added_sensors[i];
                lb.new_app.default_config[sensor.name] = sensor.config;
            }
            for (i = lb.added_actuators.length - 1; i >= 0; i--) {
                var actuator = lb.added_actuators[i];
                lb.new_app.default_config[actuator.name] = actuator.config;
            }
            var newApp = appRef.push(lb.new_app, function(err) {
                if (err) {
                    console.log(err);
                } else {
                    lb.init();
                    $window.location.hash = "";
                }
            });
        };
    }
})();
