(function() {
    'use strict';

    madAPP.controller('demoLightController', demoLightController);

    function demoLightController($timeout) {
        var vm = this;
        vm.text = 'Firebase rulez!';

        var dbRef = firebase.database().ref();
        var appid = "-KXwk9l34wEwHLVKZ6fr";
        vm.app_config = null;
        var instances = null;
        var appRef = dbRef.child('apps').child(appid);
        var install_sensors = dbRef.child('install_sensors');
        var install_actuators = dbRef.child('install_actuators');
        var ACTIVATION_VAL = 200;

        vm.light_reading = null;
        vm.flash_on = null;

        appRef.once('value').then(function(snapshot) {
            vm.app_config = snapshot.val();

            instances = _.keys(vm.app_config.instances);
        });

        var app_idsRef = dbRef.child('app_ids');

        app_idsRef.orderByChild("app_id").equalTo(appid).on('child_added', function(data) {
            console.log('new_instance', data.key);
            var new_instance = data.key;
            var install_sensorRef = install_sensors.child(new_instance);
            var install_actuatorRef = install_actuators.child(new_instance);
            var actuator = null;
            install_actuatorRef.child('flash').once("value").then(function(snapshot) {
                actuator = _.keys(snapshot.val())[0];
                console.log('flash', actuator);
            });

            install_sensorRef.child('light').on('value', function(snapshot) {
                $timeout(function() {
                    var device_id = _.keys(snapshot.val())[0];
                    vm.light_reading = _.map(_.keys(snapshot.val()), function(x) {
                        return snapshot.val()[x];
                    });
                    vm.light_reading = vm.light_reading[0].value;
                    console.log(vm.light_reading);
                    var flash = vm.light_reading < ACTIVATION_VAL;
                    console.log(flash);
                    if (flash) {
                        vm.flash_on = true;
                        install_actuatorRef.child('flash').child(actuator).set(true);
                    } else {
                        vm.flash_on = false;
                        install_actuatorRef.child('flash').child(actuator).set(false);
                    }
                }, 0);
            });
        });
    }
})();
